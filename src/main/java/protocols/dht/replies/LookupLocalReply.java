package protocols.dht.replies;

import protocols.storage.requests.StoreLocalRequest;
import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.data.Host;

import java.util.UUID;

public class LookupLocalReply extends ProtoReply {
    final public static short REPLY_ID = 983;

    private String name;
    private Host host;
    private UUID uid;
    private byte[] content;


    public LookupLocalReply (String name, Host host, byte[] content) {
        super(LookupLocalReply.REPLY_ID);
        this.name = name;
        this.host = host;
        this.uid = UUID.randomUUID();
        this.content = content;
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

    public byte[] getContent() {
        return content;
    }
}
