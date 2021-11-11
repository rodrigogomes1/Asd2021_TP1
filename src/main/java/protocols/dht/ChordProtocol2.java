package protocols.dht;

import protocols.dht.ChordTimers.StabilizeTimer;
import channel.notifications.ChannelCreated;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ChordTimers.CheckPredecessorTimer;
import protocols.dht.messages.FindSucMsg;
import protocols.dht.messages.FindSucMsgResponse;
import protocols.dht.messages.StabilizeMsg;
import protocols.dht.messages.StabilizeMsgResponse;
import protocols.dht.replies.LookupFailedReply;
import protocols.dht.replies.LookupOKReply;
import protocols.dht.requests.LookupRequest;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.*;
import pt.unl.fct.di.novasys.network.data.Host;
import utils.HashGenerator;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

public class ChordProtocol2 extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(ChordProtocol.class);
    public final static short PROTOCOL_ID = 700;
    public final static String PROTOCOL_NAME = "ChordProtocol";

    private final Host selfHost;
    private final BigInteger selfId;
    private final int stabilizeTime; //param: timeout for stabilize
    private final int checkPredecessorTime; //param: timeout for checkPredecessor
    private final int channelId;

    private Host sucHost;
    private BigInteger sucId;
    private Host preHost;
    private BigInteger preId;
    private final Set<Host> connections; //Peers I am connected to

    private boolean channelReady;

    public ChordProtocol2(Properties props, Host self) throws HandlerRegistrationException, IOException {
        super(PROTOCOL_NAME, PROTOCOL_ID);
        selfHost = self;
        selfId = HashGenerator.generateHash(self.toString());
        this.connections = new HashSet<>();

        this.stabilizeTime = Integer.parseInt(props.getProperty("stabilize_Time", "2000")); //2 seconds
        this.checkPredecessorTime = Integer.parseInt(props.getProperty("checkPredecessor_Time", "2500")); //2.5 seconds

        Properties channelProps = new Properties();
        channelProps.setProperty(TCPChannel.ADDRESS_KEY, props.getProperty("address")); //The address to bind to
        channelProps.setProperty(TCPChannel.PORT_KEY, props.getProperty("port")); //The port to bind to
        channelProps.setProperty(TCPChannel.METRICS_INTERVAL_KEY, "10000"); //The interval to receive channel metrics
        channelProps.setProperty(TCPChannel.HEARTBEAT_INTERVAL_KEY, "1000"); //Heartbeats interval for established connections
        channelProps.setProperty(TCPChannel.HEARTBEAT_TOLERANCE_KEY, "3000"); //Time passed without heartbeats until closing a connection
        channelProps.setProperty(TCPChannel.CONNECT_TIMEOUT_KEY, "1000"); //TCP connect timeout
        channelId = createChannel(TCPChannel.NAME, channelProps); //Create the channel with the given

        /*---------------------- Register Request Handlers ---------------------- */
        registerRequestHandler(LookupRequest.REQUEST_ID, this::uponLookupRequest);

        /*---------------------- Register Message Serializers ---------------------- */
        registerMessageSerializer(channelId, FindSucMsg.MSG_ID, FindSucMsg.serializer);
        registerMessageSerializer(channelId, FindSucMsgResponse.MSG_ID, FindSucMsgResponse.serializer);
        registerMessageSerializer(channelId, StabilizeMsg.MSG_ID, StabilizeMsg.serializer);
        registerMessageSerializer(channelId, StabilizeMsgResponse.MSG_ID, StabilizeMsgResponse.serializer);

        /*---------------------- Register Message Handlers ---------------------- */
        registerMessageHandler(channelId, FindSucMsg.MSG_ID, this::uponFindSucMsg);
        registerMessageHandler(channelId, FindSucMsgResponse.MSG_ID, this::uponFindSucMsgResponse);
        registerMessageHandler(channelId, StabilizeMsg.MSG_ID, this::uponStabilizeMsg);
        registerMessageHandler(channelId, StabilizeMsgResponse.MSG_ID, this::uponStabilizeMsgResponse);

        /*-------------------- Register Channel Events ------------------------------- */
        registerChannelEventHandler(channelId, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
        registerChannelEventHandler(channelId, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);
        registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
        registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
        registerChannelEventHandler(channelId, InConnectionDown.EVENT_ID, this::uponInConnectionDown);

        /*--------------------- Register Timer Handlers ----------------------------- */
        registerTimerHandler(StabilizeTimer.TIMER_ID, this::uponStabilizeTimer);
        registerTimerHandler(CheckPredecessorTimer.TIMER_ID, this::uponCheckPredecessorTimer);

        System.out.println("Constructor chord");
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        triggerNotification(new ChannelCreated(channelId));
        System.out.println("Constructor int "+selfHost);
        preHost = null;
        preId = null;

        if (props.containsKey("contact")) { //Join chord ring containing node contact
            System.out.println("Join chord ring");
            String contact = props.getProperty("contact");
            String[] hostElems = contact.split(":");
            Host contactHost = new Host(InetAddress.getByName(hostElems[0]), Short.parseShort(hostElems[1]));
            openConnection(contactHost, channelId);
            sendMessage(channelId, new FindSucMsg(selfHost, selfId), contactHost);

        }else{ //Create new chord ring
            System.out.println("Create new chord ring");
            sucHost = selfHost;
            sucId = selfId;
        }

        //Setup the timer used to do the stabilize method periodically (we registered its handler on the constructor)
        setupPeriodicTimer(new StabilizeTimer(), this.stabilizeTime, this.stabilizeTime);
        setupPeriodicTimer(new CheckPredecessorTimer(), this.checkPredecessorTime, this.checkPredecessorTime);
        System.out.println("Constructor end");
    }

    private void uponFindSucMsg(FindSucMsg msg, Host from, short sourceProto, int channelId){
        System.out.println("Received FindSucMsg {} from {}");
        BigInteger receivedId= msg.getHashId();

        if(sucId.compareTo(selfId)==0 || between(selfId, msg.getHashId(), sucId)) {
            openConnection(msg.getHost(), this.channelId);
            sendMessage(this.channelId, new FindSucMsgResponse(sucHost ,sucId), msg.getHost());
        }else {
            openConnection(sucHost, this.channelId);
            sendMessage(this.channelId, msg, sucHost);
        }
    }

    private void uponFindSucMsgResponse(FindSucMsgResponse msg, Host from, short sourceProto, int channelId){
        System.out.println("Receive sucMsgResponse");
        sucId = msg.getHashId();
        sucHost = msg.getHost();
    }

    private void uponStabilizeTimer(StabilizeTimer timer, long timerId) {
        System.out.println("Stabilize timer");
        openConnection(sucHost, this.channelId);
        sendMessage(this.channelId, new StabilizeMsg(selfHost, selfId), sucHost);
    }

    private void uponLookupRequest(LookupRequest request, short sourceProto) {
        System.out.println("Request");
        System.out.println("Suc "+sucHost+" : "+sucId);
        System.out.println("Pre "+preHost+" : "+preId);

        ProtoReply reply = null;
        reply = new LookupOKReply(request.getID(), request.getRequestUID(), request.getName(), new byte[]{});
        sendReply(reply, sourceProto);
    }

    private void uponStabilizeMsg(StabilizeMsg msg, Host from, short sourceProto, int channelId){
        System.out.println("Receive stabilize msg");
        openConnection(from, this.channelId);
        sendMessage(this.channelId, new StabilizeMsgResponse(preHost, preId), from);

        //Notify
        if(preId == null || between(preId, msg.getHashId(), selfId)){ //if bigger than my pre, new pre
            preId = msg.getHashId();
            preHost = msg.getHost();
        }

    }

    private void uponStabilizeMsgResponse(StabilizeMsgResponse msg, Host from, short sourceProto, int channelId) {
        System.out.println("Receive sucMsgResponse");
        if(between(selfId, msg.getHashId(), sucId)){ //if smaller than suc and bigger than self, new suc
            sucId = msg.getHashId();
            sucHost = msg.getHost();
        }
    }
    
    private void uponCheckPredecessorTimer(CheckPredecessorTimer timer, long timerId) {
    	System.out.println("Check predecessor timer");
    	if(!connections.contains(preHost)){
    		System.out.println("Predecessor " + preHost + " : " + preId + " failed");
    		preHost = null;
    		preId = null;
    	}
    }

    // Test if BigInteger is in the middle of other 2 "in a circle"
    // 10000 "<" 2 "<" 3 -> true
    private boolean between(BigInteger left, BigInteger middle, BigInteger right){
        if(left.compareTo(right)<0)
            return (middle.compareTo(left)>0 && middle.compareTo(right)<0);
        else
            return (middle.compareTo(left)>0 || middle.compareTo(right)<0);

    }

    //If a connection is successfully established, this event is triggered. In this protocol, we want to add the
    //respective peer to the membership, and inform the Dissemination protocol via a notification.
    private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
        Host peer = event.getNode();
        System.out.println(String.format("Connection to {} is up", peer));
        connections.add(peer);
    }

    //If an established connection is disconnected, remove the peer from the membership and inform the Dissemination
    //protocol. Alternatively, we could do smarter things like retrying the connection X times.
    private void uponOutConnectionDown(OutConnectionDown event, int channelId) {
        Host peer = event.getNode();
        System.out.println(String.format("Connection to {} is down cause {}", peer, event.getCause()));
        connections.remove(event.getNode());
    }

    //If a connection fails to be established, this event is triggered. In this protocol, we simply remove from the
    //pending set. Note that this event is only triggered while attempting a connection, not after connection.
    //Thus the peer will be in the pending set, and not in the membership (unless something is very wrong with our code)
    private void uponOutConnectionFailed(OutConnectionFailed<ProtoMessage> event, int channelId) {
    	System.out.println(String.format("Connection to {} failed cause: {}", event.getNode(), event.getCause()));
    }

    //If someone established a connection to me, this event is triggered. In this protocol we do nothing with this event.
    //If we want to add the peer to the membership, we will establish our own outgoing connection.
    // (not the smartest protocol, but its simple)
    private void uponInConnectionUp(InConnectionUp event, int channelId) {
    	System.out.println(String.format("Connection from {} is up", event.getNode()));
    }

    //A connection someone established to me is disconnected.
    private void uponInConnectionDown(InConnectionDown event, int channelId) {
    	System.out.println(String.format("Connection from {} is down, cause: {}", event.getNode(), event.getCause()));
    }


}