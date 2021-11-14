package protocols.dht.replies;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import pt.unl.fct.di.novasys.network.data.Host;

import java.util.Set;

public class StoreBulkLocalReply extends ProtoReply{
        final public static short REPLY_ID = 415;
        Host host;
        Set<String> names;

    public StoreBulkLocalReply(Host host, Set<String> names) {
        super(REPLY_ID);
        this.host = host;
        this.names = names;
    }

    public Host getHost() {
        return host;
    }

    public Set<String> getNames() {
        return names;
    }
}
