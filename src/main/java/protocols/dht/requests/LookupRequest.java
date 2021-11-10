package protocols.dht.requests;

import java.math.BigInteger;
import java.util.UUID;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class LookupRequest extends ProtoRequest {

	public final static short REQUEST_ID = 101;
	
	private BigInteger id;
	private UUID uid;
	private String name;
	
	public LookupRequest(BigInteger id, String name) {
		super(REQUEST_ID);
		this.id = id;
		this.uid = UUID.randomUUID();
		this.name = name;
	}
	
	public LookupRequest(BigInteger id, UUID uid) {
		super(REQUEST_ID);
		this.id = id;
		this.uid = uid;
	}
	
	public UUID getRequestUID() {
		return this.uid;
	}
	
	public BigInteger getID() {
		return this.id;
	}

	public String getName() {
		return name;
	}
}
