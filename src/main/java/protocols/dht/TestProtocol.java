package protocols.dht;

import channel.notifications.ChannelCreated;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protocols.dht.replies.LookupFailedReply;
import protocols.dht.replies.LookupOKReply;
import protocols.dht.requests.LookupRequest;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.util.*;

public class TestProtocol extends GenericProtocol{
    private static final Logger logger = LogManager.getLogger(TestProtocol.class);
    public final static short PROTOCOL_ID = 700;
    public final static String PROTOCOL_NAME = "TestDHT";
    private final Host self;
    private final Set<Host> neighbours;

    private boolean channelReady;

    public TestProtocol(Properties props, Host self) throws HandlerRegistrationException {
        super(PROTOCOL_NAME, PROTOCOL_ID);
        this.self=self;
        neighbours = new HashSet<>();
        channelReady = false;

        //Lookup
        registerRequestHandler(LookupRequest.REQUEST_ID, this::uponLookupRequest);

        //Channel created
        subscribeNotification(ChannelCreated.NOTIFICATION_ID, this::uponChannelCreated);
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        // TODO Auto-generated method stub

    }

    private void uponChannelCreated(ChannelCreated notification, short sourceProto) {
        int cId = notification.getChannelId();
        // Allows this protocol to receive events from this channel.
        registerSharedChannel(cId);
        /*
        /*---------------------- Register Message Serializers ---------------------- */
        //registerMessageSerializer(cId, FloodMessage.MSG_ID, FloodMessage.serializer);
        /*---------------------- Register Message Handlers -------------------------- */
        /*
        try {
            registerMessageHandler(cId, FloodMessage.MSG_ID, this::uponFloodMessage, this::uponMsgFail);
        } catch (HandlerRegistrationException e) {
            logger.error("Error registering message handler: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        */

        //Now we can start sending messages
        channelReady = true;
        logger.info("Channel ready" );
    }

    private void uponLookupRequest(LookupRequest request, short sourceProto) {
        logger.info("Receive lookup Request for content with name {}", request.getName() );
        Random rand = new Random();
        ProtoReply reply = null;

        if(rand.nextInt(2) == 1)
            reply = new LookupOKReply(request.getID(), request.getRequestUID(), request.getName(), new byte[]{});
        else
            reply = new LookupFailedReply(request.getID(), request.getRequestUID(), request.getName());

        sendReply(reply, sourceProto);
    }
}
