package protocols.dht.replies;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

import java.math.BigInteger;
import java.util.UUID;

public class LookupOKReply extends ProtoReply {

    final public static short REPLY_ID = 701;
    private UUID uid;
    private String name;
    private byte[] content;

    public LookupOKReply(String name, UUID uid, byte[] content) {
        super(REPLY_ID);
        this.name = name;
        this.content = content;
        this.uid = uid;
    }

    public UUID getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public byte[] getContent() {
        return content;
    }
}
