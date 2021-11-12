package protocols.dht.messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class FindSucMsgResponse extends ProtoMessage {

    public final static short MSG_ID = 738;

    private final Host host;
    private final BigInteger hasId;
    private final BigInteger id;

    public FindSucMsgResponse(Host host, BigInteger hasId, BigInteger id) {
        super(MSG_ID);
        this.host = host;
        this.hasId = hasId;
        this.id = id;
    }

    public Host getHost() {
        return host;
    }

    public BigInteger getHashId() {
        return id;
    }
    
    public BigInteger getTargetId() {
        return id;
    }

    @Override
    public String toString() {
        return "ShuffleMessage{" +
                "subset=" + host +
                '}';
    }

    public static ISerializer<FindSucMsgResponse> serializer = new ISerializer<FindSucMsgResponse>() {
        @Override
        public void serialize(FindSucMsgResponse msg, ByteBuf out) throws IOException {
            Host.serializer.serialize(msg.getHost(), out);
            byte[] byteHashId = msg.getHashId().toByteArray();
            byte[] byteId = msg.getTargetId().toByteArray();
            out.writeBytes(byteHashId);
            out.writeBytes(byteId);
        }

        @Override
        public FindSucMsgResponse deserialize(ByteBuf in) throws IOException {
        	Host host = Host.serializer.deserialize(in);
            byte[] byteHashId = new byte[20];
            byte[] byteId = new byte[20];
            in.readBytes(byteHashId);
            in.readBytes(byteId);
            BigInteger hashId = new BigInteger(byteHashId);
            BigInteger id = new BigInteger(byteId);
            return new FindSucMsgResponse(host, hashId, id);
        }
    };
}

