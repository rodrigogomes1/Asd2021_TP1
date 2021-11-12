package protocols.storage.requests;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

import java.util.UUID;

public class StoreLocalRequest extends ProtoRequest {
    final public static short REQUEST_ID = 258;

    private String name;
    private byte[] content;
    private UUID uid;

    public StoreLocalRequest(String name, byte[] content) {
        super(StoreLocalRequest.REQUEST_ID);
        this.name = name;
        this.content = content;
        this.uid = UUID.randomUUID();
    }

    public UUID getRequestUID() {
        return this.uid;
    }

    public String getName() {
        return this.name;
    }

    public byte[] getContent() {
        return this.content;
    }

}

