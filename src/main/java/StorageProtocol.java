import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverters.BigDecimalConverter;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverters.BigIntegerConverter;

import channel.notifications.ChannelCreated;
import protocols.dht.replies.*;
import protocols.dht.requests.LookupRequest;
import protocols.storage.replies.RetrieveFailedReply;
import protocols.storage.replies.RetrieveOKReply;
import protocols.storage.replies.StoreOKReply;
import protocols.storage.requests.*;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.network.data.Host;
import utils.HashGenerator;
import utils.HashProducer;

public class StorageProtocol extends GenericProtocol {
	//private static final Logger logger = LogManager.getLogger(StorageProtocol.class);
	public final static short PROTOCOL_ID = 600;
	public final static String PROTOCOL_NAME = "Storage";

	private final short dhtProtoId;
	private final short upProtoId;

	private final HashMap<String, byte[]> contentsMap = new HashMap<String,  byte[]>();
	private final HashMap<BigInteger, String> idToName = new HashMap<>();

	private final Host self;

	private int channelId; //Id of the created channel 
	private boolean channelReady;

	HashProducer hashProducer;

	public StorageProtocol(Properties props, Host self,short dhtProtoId, short upProtoId) throws HandlerRegistrationException, IOException {
		super(PROTOCOL_NAME, PROTOCOL_ID);
		this.self=self;
		channelId=-1;
		this.dhtProtoId = dhtProtoId;
		this.upProtoId = upProtoId; //ProtoId of protocol above, AutomatedApplication
		channelReady = false;
		hashProducer = new HashProducer(self);

		/*--------------------- Register Request Handlers ----------------------------- */
		registerRequestHandler(StoreRequest.REQUEST_ID, this::uponStoreRequest);
		registerRequestHandler(RetrieveRequest.REQUEST_ID, this::uponRetrieveRequest);
		registerRequestHandler(StoreLocalRequest.REQUEST_ID, this::uponStoreLocalRequest);
		registerRequestHandler(LookupLocalRequest.REQUEST_ID, this::uponLookupLocalRequest);
		registerRequestHandler(LookupBulkLocalRequest.REQUEST_ID, this::uponLookupBulkLocalRequest);
		registerRequestHandler(StoreBulkLocalRequest.REQUEST_ID, this::uponStoreBulkLocalRequest);
		registerRequestHandler(RemoveBulkRequest.REQUEST_ID, this::uponRemoveBulkRequest);

		 /*--------------------- Register Notification Handlers ----------------------------- */
        subscribeNotification(ChannelCreated.NOTIFICATION_ID, this::uponChannelCreated);

		/*--------------------- Register Reply Handlers ----------------------------- */
		registerReplyHandler(LookupOKReply.REPLY_ID, this::uponLookupOkReply);
		registerReplyHandler(LookupFailedReply.REPLY_ID, this::uponLookupFailedReply);
		registerReplyHandler(StoreOKReply.REPLY_ID, this::uponStoreOkReply);

	}

	@Override
	public void init(Properties props) throws HandlerRegistrationException, IOException {
		// TODO Auto-generated method stub


	}


	private void uponChannelCreated(ChannelCreated notification, short sourceProto) {
		registerSharedChannel(notification.getChannelId());
		int cId = notification.getChannelId();
		//logger.info("Channel ready {}", cId );
	}

	private void uponStoreRequest(StoreRequest request, short sourceProto) {
		//System.out.println("Upon store request");
		sendRequest(request, dhtProtoId);
	}

	private void uponStoreLocalRequest(StoreLocalRequest request, short sourceProto) {
		//System.out.println("Upon store local request");
		contentsMap.put(request.getName(), request.getContent());
		BigInteger id = HashGenerator.generateHash(request.getName());
		idToName.put(id, request.getName());
	}

	private void uponLookupLocalRequest(LookupLocalRequest request, short sourceProto){
		//System.out.println("Upon look local request");
		byte[] content= contentsMap.get(request.getName());
		if(content!=null)
			sendReply(new LookupLocalReply(request.getName(), request.getHost(),content), sourceProto);
		else
			sendReply(new LookupLocalReply(request.getName(), request.getHost(),new byte[0]), sourceProto);
	}

	private void uponLookupBulkLocalRequest(LookupBulkLocalRequest request, short sourceProto){
		Set<String> names = new HashSet<>();
		Set<byte[]> contents = new HashSet<>();
		for(BigInteger id : request.getFiles()){
			String name = idToName.get(id);
			if(id != null){
				names.add(name);
				contents.add(contentsMap.get(name));
			}
		}
		sendReply(new LookupBulkLocalReply(request.getHost(), names, contents), sourceProto);
	}
	private void uponStoreBulkLocalRequest(StoreBulkLocalRequest request, short sourceProto){
		Iterator<String> names = request.getNames().iterator();
		Iterator<byte[]> contents = request.getContents().iterator();

		while(names.hasNext() && contents.hasNext()) {
			String name = names.next();
			byte[] content = contents.next();
			contentsMap.put(name, content);
			BigInteger id = HashGenerator.generateHash(name);
			idToName.put(id, name);
		}

		sendReply(new StoreBulkLocalReply(request.getHost(), request.getNames()), sourceProto);
	}

	private void  uponRemoveBulkRequest(RemoveBulkRequest request, short sourceProto){
		for(BigInteger id: request.getIds()){
			String name = idToName.get(id);
			if(name!=null){
				contentsMap.remove(name);
				idToName.remove(id);
			}
		}
	}
	
	private void uponRetrieveRequest(RetrieveRequest request, short sourceProto) {
		//System.out.println("Upon retrieve request");
		byte[] content= contentsMap.get(request.getName());
		
		if(content!=null) {
			RetrieveOKReply retrieve= new RetrieveOKReply(request.getName(), request.getRequestUID(), content);
			sendReply(retrieve, upProtoId);
			//logger.info(" Sending Retrieve Ok Reply to App" );
		}else {
			String name = request.getName();
			BigInteger id = HashGenerator.generateHash(name);//new BigInteger(toB);
			LookupRequest lookupRequest = new LookupRequest(id, name);
			sendRequest(lookupRequest, dhtProtoId);
		}
	}

	private void uponLookupOkReply(LookupOKReply reply, short sourceProto){
		//System.out.println("Upon look ok reply");
		RetrieveOKReply retrieve = new RetrieveOKReply(reply.getName(), reply.getUid(), reply.getContent());
		sendReply(retrieve, upProtoId);
		//logger.info(" Sending Retrieve Ok Reply to App" );
	}

	private void uponLookupFailedReply(LookupFailedReply reply, short sourceProto){
		//System.out.println("Upon look failed reply");
		RetrieveFailedReply retrieve = new RetrieveFailedReply(reply.getName(), reply.getUid());
		sendReply(retrieve, upProtoId);
		//logger.info(" Sending Retrieve Failed Reply to App" );
	}

	private void uponStoreOkReply(StoreOKReply reply, short sourceProto){
		//System.out.println("Store okk reply");
		sendReply(reply, upProtoId);
	}


	/*
	private void uponLookUpResponse(LookUpReply reply, short sourceProto) {
		RetrieveOKReply retrieve= new RetrieveOKReply(request.getName(), request.getRequestUID(), content);
		//ver qual e a destination
		sendReply(retrieve, sourceProto);
		logger.info(" Sending Retrieve Ok Reply to App" );
		RetrieveFailedReply retrieve = new RetrieveFailedReply(request.getName(), request.getRequestUID());
		sendReply(retrieve, sourceProto);
		logger.info(" Sending Retrieve Failed Reply to App" );
	}
	*/
	
	
}//final
