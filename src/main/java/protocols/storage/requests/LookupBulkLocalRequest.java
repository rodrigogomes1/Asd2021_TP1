package protocols.storage.requests;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.data.Host;

import java.math.BigInteger;
import java.util.Set;


public class LookupBulkLocalRequest extends ProtoRequest {
    final public static short REQUEST_ID = 736;

    private Host host;
    private Set<BigInteger> files;


    public LookupBulkLocalRequest(Host host, Set<BigInteger> files) {
        super(LookupBulkLocalRequest.REQUEST_ID);
        this.host = host;
        this.files = files;
    }

    public Host getHost() {
        return host;
    }

    public Set<BigInteger> getFiles() {
        return files;
    }
}

