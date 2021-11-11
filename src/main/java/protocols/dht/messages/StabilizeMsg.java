package protocols.dht.messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.math.BigInteger;

public class StabilizeMsg extends ProtoMessage {

    public final static short MSG_ID = 737;

    private final Host host;
    private final BigInteger id;

    public StabilizeMsg(Host host, BigInteger id) {
        super(MSG_ID);
        this.host = host;
        this.id = id;
    }

    public Host getHost() {
        return host;
    }

    public BigInteger getHashId() {
        return id;
    }

    @Override
    public String toString() {
        return "ShuffleMessage{" +
                "subset=" + host +
                '}';
    }

    public static ISerializer< StabilizeMsg> serializer = new ISerializer< StabilizeMsg>() {
        @Override
        public void serialize( StabilizeMsg msg, ByteBuf out) throws IOException {
            Host.serializer.serialize(msg.getHost(), out);
            byte[] byteId = msg.getHashId().toByteArray();
            out.writeBytes(byteId);
        }

        @Override
        public  StabilizeMsg deserialize(ByteBuf in) throws IOException {
            Host host = Host.serializer.deserialize(in);
            byte[] byteId = new byte[20];
            in.readBytes(byteId);
            BigInteger id = new BigInteger(byteId);
            return new  StabilizeMsg(host, id);
        }
    };
}
