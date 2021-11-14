package protocols.dht.replies;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import pt.unl.fct.di.novasys.network.data.Host;

import java.util.Set;

public class LookupBulkLocalReply  extends ProtoReply {
    final public static short REPLY_ID = 956;
    Host host;
    Set<String> names;
    Set<byte[]> contents;

    public LookupBulkLocalReply (Host host, Set<String> names, Set<byte[]> contents) {
        super(LookupBulkLocalReply.REPLY_ID);
        this.host = host;
        this.names = names;
        this.contents = contents;
    }

    public Host getHost() {
        return host;
    }

    public Set<String> getNames() {
        return names;
    }

    public Set<byte[]> getContents() {
        return contents;
    }
}
