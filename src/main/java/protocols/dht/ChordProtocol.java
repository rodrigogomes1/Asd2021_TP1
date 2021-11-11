package protocols.dht;

import Messages.*;
import channel.notifications.ChannelCreated;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import protocols.dht.ChordTimers.StabilizeTimer;
import protocols.dht.requests.LookupRequest;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionDown;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionUp;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionDown;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionFailed;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionUp;
import pt.unl.fct.di.novasys.network.data.Host;
import utils.HashGenerator;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

public class ChordProtocol extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(ChordProtocol.class);
    public final static short PROTOCOL_ID = 700;
    public final static String PROTOCOL_NAME = "ChordProtocol";

    private final Host self;
    private boolean channelReady;
    private final int channelId; //Id of the created channel
    private final Set<Host> pending; //Peers I am trying to connect to
    private final int stabilizeTime; //param: timeout for stabilize
    
    private Hashtable<BigInteger, Host> fingerTable;
    
    private BigInteger predecessorId;
    private Host predecessorHost;
    private BigInteger successorId;
    private Host successorHost;
    private BigInteger idLocalNode;

    public ChordProtocol(Properties props, Host self) throws HandlerRegistrationException, IOException {
        super(PROTOCOL_NAME, PROTOCOL_ID);
        this.self=self;
        channelReady = false;
        this.pending = new HashSet<>();
        
        idLocalNode = HashGenerator.generateHash(self.toString());
        
        this.stabilizeTime = Integer.parseInt(props.getProperty("stabilize_Time", "2000")); //2 seconds

        //Lookup
        registerRequestHandler(LookupRequest.REQUEST_ID, this::uponLookupRequest);

        //Channel created
        //subscribeNotification(ChannelCreated.NOTIFICATION_ID, this::uponChannelCreated);
       

       Properties channelProps = new Properties();
        channelProps.setProperty(TCPChannel.ADDRESS_KEY, props.getProperty("address")); //The address to bind to
        channelProps.setProperty(TCPChannel.PORT_KEY, props.getProperty("port")); //The port to bind to
        channelProps.setProperty(TCPChannel.METRICS_INTERVAL_KEY, "10000"); //The interval to receive channel metrics
        channelProps.setProperty(TCPChannel.HEARTBEAT_INTERVAL_KEY, "1000"); //Heartbeats interval for established connections
        channelProps.setProperty(TCPChannel.HEARTBEAT_TOLERANCE_KEY, "3000"); //Time passed without heartbeats until closing a connection
        channelProps.setProperty(TCPChannel.CONNECT_TIMEOUT_KEY, "1000"); //TCP connect timeout
        channelId = createChannel(TCPChannel.NAME, channelProps); //Create the channel with the given
        
        /*-------------------- Register Channel Events ------------------------------- */
        registerChannelEventHandler(channelId, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
        registerChannelEventHandler(channelId, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);
        registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
        registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
        registerChannelEventHandler(channelId, InConnectionDown.EVENT_ID, this::uponInConnectionDown);
        
        /*---------------------- Register Message Serializers ---------------------- */     
        registerMessageSerializer(channelId, JoinMessage.MSG_ID, JoinMessage.serializer);
        registerMessageSerializer(channelId, SuccessorToJoinFoundMessage.MSG_ID, SuccessorToJoinFoundMessage.serializer);
        registerMessageSerializer(channelId, StabilizeRequestMessage.MSG_ID, StabilizeRequestMessage.serializer);
        registerMessageSerializer(channelId, StabilizeResponseMessage.MSG_ID, StabilizeResponseMessage.serializer);
        registerMessageSerializer(channelId, NotifyMessage.MSG_ID, NotifyMessage.serializer);

        /*---------------------- Register Message Handlers ---------------------- */    
        registerMessageHandler(channelId, JoinMessage.MSG_ID, this::uponJoinMessageReceive);
        registerMessageHandler(channelId, SuccessorToJoinFoundMessage.MSG_ID, this::uponJoinFoundMessageReceive);
        registerMessageHandler(channelId, StabilizeRequestMessage.MSG_ID, this::uponStabilizeRequestMessageReceive);
        registerMessageHandler(channelId, StabilizeResponseMessage.MSG_ID, this::uponStabilizeResponseMessageReceive);
        registerMessageHandler(channelId, NotifyMessage.MSG_ID, this::uponNotifyMessageReceive);

        /*--------------------- Register Timer Handlers ----------------------------- */
        registerTimerHandler(StabilizeTimer.TIMER_ID, this::uponStabilizeTimer);

       
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
    	 triggerNotification(new ChannelCreated(channelId));
    	 
    	 if (props.containsKey("contact")) {
             try {
                 String contact = props.getProperty("contact");
                 String[] hostElems = contact.split(":");
                 Host contactHost = new Host(InetAddress.getByName(hostElems[0]), Short.parseShort(hostElems[1]));
                 //We add to the pending set until the connection is successful
                 pending.add(contactHost);
                 openConnection(contactHost);
                 predecessorId=null;
                 predecessorHost=null;
                 sendMessage(new JoinMessage(self,idLocalNode), contactHost);
                              
             } catch (Exception e) {
                 logger.error("Invalid contact on configuration: '" + props.getProperty("contacts"));
                 e.printStackTrace();
                 System.exit(-1);
             }
         }else {
    	        predecessorId=null;
    	        predecessorHost=null;
	     		successorId=HashGenerator.generateHash(self.toString());
	     		successorHost=self;
         }
    	 
    	//Setup the timer used to do the stabilize method periodically (we registered its handler on the constructor)
         setupPeriodicTimer(new StabilizeTimer(), this.stabilizeTime, this.stabilizeTime);
    	
    }
    
    /*--------------------------------- Messages ---------------------------------------- */
    private void uponJoinMessageReceive(JoinMessage msg, Host from, short sourceProto, int channelId) {
        //Received a sample from a peer. We add all the unknown peers to the "pending" map and attempt to establish
        //a connection. If the connection is successful, we add the peer to the membership (in the connectionUp callback)
    	logger.debug("Received JoinMessage {} from {}", msg, from);
    	
    	BigInteger receivedId= msg.getpeerId();
    	
    	if( receivedId.compareTo(successorId) < 0 && receivedId.compareTo(idLocalNode)>0 ) {
    		sendMessage(new SuccessorToJoinFoundMessage(successorHost ,successorId), msg.getOriginalSender());
    	}else {
    		
    		sendMessage(msg, successorHost);
    	}
    }
    
    private void uponJoinFoundMessageReceive(SuccessorToJoinFoundMessage msg, Host from, short sourceProto, int channelId) {
    	
    	logger.debug("Received SuccessorToJoinFoundMessage {} from {}", msg, from);
    	
    	successorHost=msg.getsuccessorHost();
    	successorId= msg.getsuccessorId();
    	
    	logger.debug("Successor of peer {} changed to host {} with id {}",idLocalNode, successorHost,successorId);
    }

    //Send Stabilize request to successor
    private void uponStabilizeTimer(StabilizeTimer timer, long timerId) {
       	logger.debug("Tries to do stabilize");
       	sendMessage(new StabilizeRequestMessage(self), successorHost);
    }

    // Receive stabilize request, sends predecessor to node that asked
    private void uponStabilizeRequestMessageReceive(StabilizeRequestMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received StabilizeRequesteMsg {} from {}", msg, from);
        sendMessage(new StabilizeResponseMessage(predecessorHost, predecessorId), from);
    }

    // Receive stabilize answer
    private void uponStabilizeResponseMessageReceive(StabilizeResponseMessage msg, Host from, short sourceProto, int channelId){
        logger.debug("Received StabilizeResponseMsg {} from {}", msg, from);
        if (msg.getPredecessorId().compareTo(successorId) < 0){
            //New successor
            successorId = msg.getPredecessorId();
            successorHost = msg.getPredecessorHost();

            //Notify new successor
            sendMessage(new NotifyMessage(self, idLocalNode), successorHost);
        }
    }

    // Receive notify message
    private void uponNotifyMessageReceive(NotifyMessage msg, Host from, short sourceProto, int channelId){
        logger.debug("Received NotifyMsg {} from {}", msg, from);
        if(predecessorId == null && msg.getNotifyId().compareTo(predecessorId)>0){
            predecessorId = msg.getNotifyId();
            predecessorHost = msg.getHost();
        }
    }
    

  //If a connection is successfully established, this event is triggered. In this protocol, we want to add the
    //respective peer to the membership, and inform the Dissemination protocol via a notification.
    private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
        Host peer = event.getNode();
        logger.debug("Connection to {} is up", peer);
        pending.remove(peer);
    }
    
  //If an established connection is disconnected, remove the peer from the membership and inform the Dissemination
    //protocol. Alternatively, we could do smarter things like retrying the connection X times.
    private void uponOutConnectionDown(OutConnectionDown event, int channelId) {
        Host peer = event.getNode();
        logger.debug("Connection to {} is down cause {}", peer, event.getCause());
    }
    
  //If a connection fails to be established, this event is triggered. In this protocol, we simply remove from the
    //pending set. Note that this event is only triggered while attempting a connection, not after connection.
    //Thus the peer will be in the pending set, and not in the membership (unless something is very wrong with our code)
    private void uponOutConnectionFailed(OutConnectionFailed<ProtoMessage> event, int channelId) {
        logger.debug("Connection to {} failed cause: {}", event.getNode(), event.getCause());
        pending.remove(event.getNode());
    }
    
    //If someone established a connection to me, this event is triggered. In this protocol we do nothing with this event.
    private void uponInConnectionUp(InConnectionUp event, int channelId) {
        logger.trace("Connection from {} is up", event.getNode());
    }
    
    //A connection someone established to me is disconnected.
    private void uponInConnectionDown(InConnectionDown event, int channelId) {
        logger.trace("Connection from {} is down, cause: {}", event.getNode(), event.getCause());
    }

    private void uponLookupRequest(LookupRequest request, short sourceProto) {
        logger.info("Receive lookup Request for content with name {}", request.getName() );
    }

}
