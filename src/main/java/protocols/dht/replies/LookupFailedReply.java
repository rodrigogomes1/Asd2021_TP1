package protocols.dht.replies;

import protocols.storage.replies.RetrieveFailedReply;
import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

import java.math.BigInteger;
import java.util.UUID;

public class LookupFailedReply extends ProtoReply {

    final public static short REPLY_ID = 702;
    private UUID uid;
    private String name;

    public LookupFailedReply(String name, UUID uid) {
        super(LookupFailedReply.REPLY_ID);
        this.uid = uid;
        this.name = name;
    }


    public UUID getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }
}
