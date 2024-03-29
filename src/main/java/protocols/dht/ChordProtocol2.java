package protocols.dht;

import protocols.dht.ChordTimers.CheckPredecessorTimer;
import protocols.dht.ChordTimers.FixFingersTimer;
import protocols.dht.ChordTimers.StabilizeTimer;
import channel.notifications.ChannelCreated;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import protocols.dht.messages.*;
import protocols.dht.replies.*;
import protocols.dht.requests.LookupRequest;
import protocols.storage.replies.RetrieveFailedReply;
import protocols.storage.replies.RetrieveOKReply;
import protocols.storage.replies.StoreOKReply;
import protocols.storage.requests.*;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.*;
import pt.unl.fct.di.novasys.network.data.Host;
import utils.HashGenerator;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

public class ChordProtocol2 extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(ChordProtocol.class);
    public final static short PROTOCOL_ID = 700;
    public final static String PROTOCOL_NAME = "ChordProtocol";

    private final short storageProtoId;


    private final Host selfHost;
    private final BigInteger selfId;
    private final int stabilizeTime; //param: timeout for stabilize
    private final int checkPredecessorTime; //param: timeout for checkPredecessor
    private final int fixFingersTime; //param: timeout for fixFingers
    private final int channelId;

    private Hashtable<BigInteger, Host> fingerTable;
    
    private Host sucHost;
    private BigInteger sucId;
    private Host preHost;
    private BigInteger preId;
    private final Set<Host> connections; //Peers I am connected to
    private Set<BigInteger> filesLocal;

    private int next;

    public ChordProtocol2(Properties props, Host self, short storageProtoId) throws HandlerRegistrationException, IOException {
        super(PROTOCOL_NAME, PROTOCOL_ID);
        selfHost = self;
        selfId = HashGenerator.generateHash(self.toString());
        this.storageProtoId = storageProtoId;
        this.connections = new HashSet<>();
        this.filesLocal = new HashSet<>();

        this.stabilizeTime = Integer.parseInt(props.getProperty("stabilize_Time", "2000")); //2 seconds
        this.checkPredecessorTime = Integer.parseInt(props.getProperty("checkPredecessor_Time", "2500")); //2.5 seconds
        this.fixFingersTime = Integer.parseInt(props.getProperty("fixFingers_Time", "2250")); //2.5 seconds
        
        next=0;

        fingerTable = new Hashtable<>();

        Properties channelProps = new Properties();
        channelProps.setProperty(TCPChannel.ADDRESS_KEY, props.getProperty("address")); //The address to bind to
        channelProps.setProperty(TCPChannel.PORT_KEY, props.getProperty("port")); //The port to bind to
        channelProps.setProperty(TCPChannel.METRICS_INTERVAL_KEY, "10000"); //The interval to receive channel metrics
        channelProps.setProperty(TCPChannel.HEARTBEAT_INTERVAL_KEY, "1000"); //Heartbeats interval for established connections
        channelProps.setProperty(TCPChannel.HEARTBEAT_TOLERANCE_KEY, "3000"); //Time passed without heartbeats until closing a connection
        channelProps.setProperty(TCPChannel.CONNECT_TIMEOUT_KEY, "1000"); //TCP connect timeout
        channelId = createChannel(TCPChannel.NAME, channelProps); //Create the channel with the given

        /*---------------------- Register Request Handlers ---------------------- */
        registerRequestHandler(StoreRequest.REQUEST_ID, this::uponStoreRequest);
        registerRequestHandler(LookupRequest.REQUEST_ID, this::uponLookupRequest);

        /*---------------------- Register Message Serializers ---------------------- */
        registerMessageSerializer(channelId, FindSucMsg.MSG_ID, FindSucMsg.serializer);
        registerMessageSerializer(channelId, FindSucMsgResponse.MSG_ID, FindSucMsgResponse.serializer);
        registerMessageSerializer(channelId, StabilizeMsg.MSG_ID, StabilizeMsg.serializer);
        registerMessageSerializer(channelId, StabilizeMsgResponse.MSG_ID, StabilizeMsgResponse.serializer);
        registerMessageSerializer(channelId, StoreMsg.MSG_ID, StoreMsg.serializer);
        registerMessageSerializer(channelId, StoreSuccessMsg.MSG_ID, StoreSuccessMsg.serializer);
        registerMessageSerializer(channelId, LookupMsg.MSG_ID, LookupMsg.serializer);
        registerMessageSerializer(channelId, LookupResponseMsg.MSG_ID, LookupResponseMsg.serializer);
        registerMessageSerializer(channelId, StoreBulkMsg.MSG_ID, StoreBulkMsg.serializer);
        registerMessageSerializer(channelId, ConfirmBulkStoreMsg.MSG_ID, ConfirmBulkStoreMsg.serializer);
        registerMessageSerializer(channelId, AskForContentMsg.MSG_ID, AskForContentMsg.serializer);

        /*---------------------- Register Message Handlers ---------------------- */
        registerMessageHandler(channelId, FindSucMsg.MSG_ID, this::uponFindSucMsg);
        registerMessageHandler(channelId, FindSucMsgResponse.MSG_ID, this::uponFindSucMsgResponse);
        registerMessageHandler(channelId, StabilizeMsg.MSG_ID, this::uponStabilizeMsg);
        registerMessageHandler(channelId, StabilizeMsgResponse.MSG_ID, this::uponStabilizeMsgResponse);
        registerMessageHandler(channelId, StoreMsg.MSG_ID, this::uponStoreMsg);
        registerMessageHandler(channelId, StoreSuccessMsg.MSG_ID, this::uponStoreSuccessMsg);
        registerMessageHandler(channelId, LookupMsg.MSG_ID, this::uponLookupMsg);
        registerMessageHandler(channelId, LookupResponseMsg.MSG_ID, this::uponLookupResponseMsg);
        registerMessageHandler(channelId, StoreBulkMsg.MSG_ID, this::uponStoreBulkMsg);
        registerMessageHandler(channelId, ConfirmBulkStoreMsg.MSG_ID, this::uponConfirmBulkStoreMsg);
        registerMessageHandler(channelId, AskForContentMsg.MSG_ID, this::uponAskForContentMsg);

        /*-------------------- Register Channel Events ------------------------------- */
        registerChannelEventHandler(channelId, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
        registerChannelEventHandler(channelId, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);
        registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
        registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
        registerChannelEventHandler(channelId, InConnectionDown.EVENT_ID, this::uponInConnectionDown);

        /*--------------------- Register Timer Handlers ----------------------------- */
        registerTimerHandler(StabilizeTimer.TIMER_ID, this::uponStabilizeTimer);
        registerTimerHandler(CheckPredecessorTimer.TIMER_ID, this::uponCheckPredecessorTimer);
        registerTimerHandler(FixFingersTimer.TIMER_ID, this::uponFixFingersTimer);

        /*--------------------- Register Reply Handlers ----------------------------- */
        registerReplyHandler(LookupLocalReply.REPLY_ID, this::uponLookupLocalReply);
        registerReplyHandler(LookupBulkLocalReply.REPLY_ID, this::uponLookupBulkLocalReply);
        registerReplyHandler(StoreBulkLocalReply.REPLY_ID, this::uponStoreBulkLocalReply);

        //System.out.println("Constructor chord");
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        triggerNotification(new ChannelCreated(channelId));
        //System.out.println("Constructor int "+selfHost);
        preHost = null;
        preId = null;

        String contact = props.getProperty("contact");
        String[] hostElems = contact.split(":");
        if (props.containsKey("contact") && !contact.equals(selfHost.toString())) { //Join chord ring containing node contact
            Host contactHost = new Host(InetAddress.getByName(hostElems[0]), Short.parseShort(hostElems[1]));
            System.out.println("Join chord ring: "+contact);
            System.out.println("This host: "+selfHost);
            openConnection(contactHost, channelId);
            //System.out.println("Send msg findSucMsg");
            sendMessage(channelId, new FindSucMsg(selfHost, selfId, selfId), contactHost);

        }else{ //Create new chord ring
            System.out.println("Create new chord ring: "+selfHost);
            sucHost = selfHost;
            sucId = selfId;
        }

        //Setup the timer used to do the stabilize method periodically (we registered its handler on the constructor)
        setupPeriodicTimer(new StabilizeTimer(), this.stabilizeTime, this.stabilizeTime);
        setupPeriodicTimer(new CheckPredecessorTimer(), this.checkPredecessorTime, this.checkPredecessorTime);
        setupPeriodicTimer(new FixFingersTimer(), this.fixFingersTime, this.fixFingersTime);
        //System.out.println("Constructor end");
    }

    private void uponFindSucMsg(FindSucMsg msg, Host from, short sourceProto, int channelId){
        //System.out.println("Received FindSucMsg {} from {}");
        BigInteger targetId= msg.getTargetId();

        if(sucId.compareTo(selfId)==0 || between(selfId, targetId, sucId)) {
            openConnection(msg.getHost(), this.channelId);
            //System.out.println("Send msg findSucMsgResponse");
            sendMessage(this.channelId, new FindSucMsgResponse(sucHost, sucId,targetId), msg.getHost());
       }else {
            Host destinyHost = findNodeFile(targetId);
            //System.out.println("Resend Find: "+ms.ge);
            //System.out.println("Resend " + selfHost.getPort()+" -> "+destinyHost.getPort());
            sendMessage(this.channelId, msg,destinyHost);
        }
    }
    
    private void uponFindSucMsgResponse(FindSucMsgResponse msg, Host from, short sourceProto, int channelId){
        //System.out.println("Receive sucMsgResponse"+(msg.getTargetId()==selfId));
        if(selfId.compareTo(msg.getTargetId())==0) {
            sucId = msg.getHashId();
            sucHost = msg.getHost();
            //System.out.println("Send msg askforcontent");
            sendMessage(this.channelId, new AskForContentMsg(selfId),sucHost);
            
        }else {
            fingerTable.put(msg.getTargetId(), msg.getHost());
        }
    }
    
    
   private void uponAskForContentMsg(AskForContentMsg msg, Host from, short sourceProto, int channelId) {
	   Set<BigInteger> files = new HashSet<>();
       for(BigInteger fileId: filesLocal)
           if(fileId.compareTo(msg.getHashId())<1 || fileId.compareTo(selfId)>1)
               files.add(fileId);
       if(!files.isEmpty()) {
           //System.out.println("Bulk lookup request");
           sendRequest(new LookupBulkLocalRequest(from, files), storageProtoId);
       }
   }

    private void uponLookupBulkLocalReply(LookupBulkLocalReply reply, short sourceProto){
        //System.out.println("Bulk lookup reply");
        sendMessage(this.channelId, new StoreBulkMsg(reply.getNames(), reply.getContents()), reply.getHost());
    }

    private void uponStoreBulkMsg(StoreBulkMsg msg, Host from, short sourceProto, int channelId) {
        //System.out.println("Bulk store msg");
        sendRequest(new StoreBulkLocalRequest(from, msg.getNames(), msg.getContents()), storageProtoId);
    }

    private void uponStoreBulkLocalReply(StoreBulkLocalReply reply, short sourceProto){
        //System.out.println("Bulk store reply");
        Set<BigInteger> ids = new HashSet<>(reply.getNames().size());
        for(String name : reply.getNames()) {
            BigInteger id = HashGenerator.generateHash(name);
            ids.add(id);
            filesLocal.add(id);
        }
        //System.out.println("Send msg confirmbulkstore");
        sendMessage(this.channelId, new ConfirmBulkStoreMsg(ids), reply.getHost());
    }

    private void uponConfirmBulkStoreMsg(ConfirmBulkStoreMsg msg, Host from, short sourceProto, int channelId) {
        System.out.println("Bulk confirm msg");
        for(BigInteger id : msg.getFileIds())
            filesLocal.remove(id);
        sendRequest(new RemoveBulkRequest(msg.getFileIds()), storageProtoId);
    }

    
    private void uponStabilizeTimer(StabilizeTimer timer, long timerId) {
        if(sucHost != null) {
            openConnection(sucHost, this.channelId);
            //System.out.println("Send msg stabilizemsg");
            sendMessage(this.channelId, new StabilizeMsg(selfHost, selfId), sucHost);
        }
    }

    private void uponStabilizeMsg(StabilizeMsg msg, Host from, short sourceProto, int channelId){
        //System.out.println("Receive stabilize msg");
        openConnection(from, this.channelId);

        //Notify
        if(preId == null || between(preId, msg.getHashId(), selfId)){ //if bigger than my pre, new pre
            preId = msg.getHashId();
            preHost = msg.getHost();
        }
        //System.out.println("Send msg stabilizeResponse");
        sendMessage(this.channelId, new StabilizeMsgResponse(preHost, preId), from);
    }
    
    private void uponStabilizeMsgResponse(StabilizeMsgResponse msg, Host from, short sourceProto, int channelId) {
        //System.out.println("Receive sucMsgResponse");
        if(between(selfId, msg.getHashId(), sucId)){ //if smaller than suc and bigger than self, new suc
            sucId = msg.getHashId();
            sucHost = msg.getHost();
        }
    }
   
    
    
    private void uponFixFingersTimer(FixFingersTimer timer, long timerId) {
        //System.out.println("FixFingers Timer");
        int m=2;
        next= next + 1;

        if(next>m) {
            next=1;
        }

        BigInteger index = BigDecimal.valueOf( Math.pow(2, next-1 ) ).toBigInteger();
        BigInteger targetKey= selfId.add(index);

        Host contactHost=findNodeFile(targetKey);
        if(contactHost != null)
            if(contactHost.equals(sucHost)) {
                fingerTable.put(targetKey, sucHost);
            }else {
                openConnection(contactHost, channelId);
                //System.out.println("Send FindSucMsg from finger");
                sendMessage(channelId, new FindSucMsg(selfHost, selfId, targetKey), contactHost);
            }

    }

    private void uponStoreRequest(StoreRequest request, short sourceProto) {
        //System.out.println("Store: " + request.getName());
        //System.out.println("Suc " + sucHost);
        BigInteger fileId = HashGenerator.generateHash(request.getName());
        if (preId == null || between(preId, fileId, selfId)) { ///se estiver entre os dois guarda no sucessor
            sendRequest(new StoreLocalRequest(request.getName(), request.getContent()), storageProtoId);
            sendReply(new StoreOKReply(request.getName(), request.getRequestUID()), storageProtoId);
            filesLocal.add(fileId);
            System.out.println("Store "+fileId+" in "+selfHost.getPort());
        } else if (sucHost != null){
            Host targetHost = findNodeFile(fileId);
            //System.out.println("Send msg storeMsg");
            sendMessage(this.channelId,
                    new StoreMsg(selfHost, fileId, request.getName(), request.getContent(), request.getRequestUID()), targetHost);
        }
    }

    private void uponStoreMsg(StoreMsg msg, Host from, short sourceProto, int channelId){
        if(between(preId, msg.getFileId(), selfId)) {
            sendRequest(new StoreLocalRequest(msg.getName(), msg.getContent()), storageProtoId);
            //System.out.println("Send msg storeSuccess");
            sendMessage(this.channelId, new StoreSuccessMsg(msg.getName(), msg.getUid()), msg.getHost());
            filesLocal.add(msg.getFileId());
            //System.out.println("Store here: "+msg.getName());
        }
        else if(sucHost != null) {
            Host targetHost = findNodeFile(msg.getFileId());
            //System.out.println("Send msg resend storeMsg");
            sendMessage(this.channelId, msg, targetHost);
        }
    }

    private void uponStoreSuccessMsg(StoreSuccessMsg msg, Host from, short sourceProto, int channelId) {
        sendReply(new StoreOKReply(msg.getName(), msg.getUid()), storageProtoId);
    }

    private void uponLookupRequest(LookupRequest request, short sourceProto) {
        //System.out.println("Request");
        //System.out.println("Suc "+sucHost);
        //System.out.println("Pre "+preHost);
        //for(Host host : fingerTable.values())
        //    System.out.println("Finger "+host);

        //StorageProtocol verifies that is not store locally
        //System.out.println("Lookup request: "+sucHost);
        for(BigInteger id : filesLocal)
            System.out.println(id);

        Host targetHost = findNodeFile(request.getID());
        //System.out.println("Send msg lookupMsg");
        sendMessage(this.channelId, new LookupMsg(selfHost, request.getID(), request.getName(), request.getRequestUID()), targetHost);
    }

    private void uponLookupMsg(LookupMsg msg, Host from, short sourceProto, int channelId){
        //System.out.println("Lookup msg");
        if(preId!= null && between(preId, msg.getFileId(), selfId)) {
            if(filesLocal.contains(msg.getFileId())){
                //System.out.println("Lookup msg contain ask protocol");
                sendRequest(new LookupLocalRequest(msg.getName(), msg.getHost()), storageProtoId);
            }else{
                //System.out.println("Lookup msg resend");
                //System.out.println("Not found");
                //System.out.println(msg.getName());
                //System.out.println("Send msg lookResponse failed");
                sendMessage(this.channelId, new LookupResponseMsg(msg.getName(), msg.getUid(), new byte[0]), msg.getHost());
            }
        }
        else{
            //System.out.println(sucHost);
            Host targetHost = findNodeFile(msg.getFileId());
            if(targetHost.compareTo(selfHost) != 0) {
                System.out.println("Resend Find: " + msg.getFileId());
                System.out.println("Resend " + selfHost.getPort() + " -> " + targetHost.getPort());
                sendMessage(this.channelId, msg, targetHost);
            }
        }
    }

    private void uponLookupLocalReply(LookupLocalReply reply, short sourceProto){
        //System.out.println("Storage answer, send to host");
        //System.out.println("Send msg lookupResponse ok");
        sendMessage(this.channelId, new LookupResponseMsg(reply.getName(), reply.getRequestUID(), reply.getContent()), reply.getHost());
    }

    private void uponLookupResponseMsg(LookupResponseMsg msg,  Host from, short sourceProto, int channelId){
        //System.out.println("Receive reply lookup: "+msg.getContent().length);
        byte[] content = msg.getContent();
        //System.out.println("Found??: "+content.length);
        //System.out.println(msg.getName());

        if(content.length == 0)
            sendReply(new LookupFailedReply(msg.getName(), msg.getUid()), storageProtoId);
        else
            sendReply(new LookupOKReply(msg.getName(), msg.getUid(), content), storageProtoId);
    }

    private void uponCheckPredecessorTimer(CheckPredecessorTimer timer, long timerId) {
        //System.out.println("Check predecessor timer");
        if(!connections.contains(preHost)){
            //System.out.println("Predecessor " + preHost + " : " + preId + " failed");
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

    private Host findNodeFile(BigInteger id){
        Host host = sucHost;
        BigInteger maxId = sucId;

        for(var finger : fingerTable.entrySet()){
            if(between(maxId,finger.getKey(), id)){
                maxId = finger.getKey();
                host = finger.getValue();
            }
        }

        return host;
    }

    //If a connection is successfully established, this event is triggered. In this protocol, we want to add the
    //respective peer to the membership, and inform the Dissemination protocol via a notification.
    private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
        Host peer = event.getNode();
        //System.out.println(String.format("Connection to {} is up", peer));
        connections.add(peer);
    }

    //If an established connection is disconnected, remove the peer from the membership and inform the Dissemination
    //protocol. Alternatively, we could do smarter things like retrying the connection X times.
    private void uponOutConnectionDown(OutConnectionDown event, int channelId) {
        Host peer = event.getNode();
        //System.out.println(String.format("Connection to {} is down cause {}", peer, event.getCause()));
        connections.remove(event.getNode());
    }

    //If a connection fails to be established, this event is triggered. In this protocol, we simply remove from the
    //pending set. Note that this event is only triggered while attempting a connection, not after connection.
    //Thus the peer will be in the pending set, and not in the membership (unless something is very wrong with our code)
    private void uponOutConnectionFailed(OutConnectionFailed<ProtoMessage> event, int channelId) {
        //System.out.println(String.format("Connection to {} failed cause: {}", event.getNode(), event.getCause()));
    }

    //If someone established a connection to me, this event is triggered. In this protocol we do nothing with this event.
    //If we want to add the peer to the membership, we will establish our own outgoing connection.
    // (not the smartest protocol, but its simple)
    private void uponInConnectionUp(InConnectionUp event, int channelId) {
        //System.out.println(String.format("Connection from {} is up", event.getNode()));
    }

    //A connection someone established to me is disconnected.
    private void uponInConnectionDown(InConnectionDown event, int channelId) {
        //System.out.println(String.format("Connection from {} is down, cause: {}", event.getNode(), event.getCause()));
    }


}