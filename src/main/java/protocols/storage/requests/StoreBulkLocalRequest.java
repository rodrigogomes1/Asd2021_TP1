package protocols.storage.requests;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.data.Host;

import java.util.Set;

public class StoreBulkLocalRequest extends ProtoRequest {
    final public static short REQUEST_ID = 761;
    Host host;
    Set<String> names;
    Set<byte[]> contents;

    public StoreBulkLocalRequest(Host host, Set<String> names, Set<byte[]> contents) {
        super(REQUEST_ID);
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
