package protocols.storage.requests;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.data.Host;

import java.util.UUID;


public class LookupLocalRequest extends ProtoRequest {
    final public static short REQUEST_ID = 754;

    private String name;
    private Host host;
    private UUID uid;


    public LookupLocalRequest(String name, Host host) {
        super(StoreLocalRequest.REQUEST_ID);
        this.name = name;
        this.host = host;
        this.uid = UUID.randomUUID();
    }

    public UUID getRequestUID() {
        return this.uid;
    }

    public String getName() {
        return this.name;
    }

    public Host getHost() {
        return this.host;
    }

}
