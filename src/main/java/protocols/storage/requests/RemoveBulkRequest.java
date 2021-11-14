package protocols.storage.requests;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

import java.math.BigInteger;
import java.util.Set;

public class RemoveBulkRequest extends ProtoRequest {
    final public static short REQUEST_ID = 427;
    Set<BigInteger> ids;

    public RemoveBulkRequest(Set<BigInteger> ids) {
        super(REQUEST_ID);
        this.ids = ids;
    }

    public Set<BigInteger> getIds() {
        return ids;
    }
}
