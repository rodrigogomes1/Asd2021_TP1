package Messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.math.BigInteger;

public class StabilizeResponseMessage extends ProtoMessage {
    public static final short MSG_ID = 205;

    private final Host predecessorHost;
    private final BigInteger predecessorId;

    @Override
    public String toString() {
        return "StabilizeResponseMessage{" +
                "mid=" +
                '}';
    }

    public StabilizeResponseMessage( Host predecessorHost, BigInteger predecessorId) {
        super(MSG_ID);
        this.predecessorHost = predecessorHost;
        this.predecessorId= predecessorId;
        //this.hash = hash;
    }

    public Host getPredecessorHost() {
        return predecessorHost;
    }

    public BigInteger getPredecessorId() {
        return predecessorId;
    }


    public static ISerializer<StabilizeResponseMessage> serializer = new ISerializer<>() {
        @Override
        public void serialize(StabilizeResponseMessage finishJoinMessage, ByteBuf out) throws IOException {
            //out.writeLong(floodMessage.mid.getMostSignificantBits());
            //out.writeLong(floodMessage.mid.getLeastSignificantBits());
            Host.serializer.serialize(finishJoinMessage.predecessorHost, out);

        }

        @Override
        public StabilizeResponseMessage deserialize(ByteBuf in) throws IOException {
            Host predecessorHost = Host.serializer.deserialize(in);
            BigInteger id= BigInteger.valueOf(1);



            return new StabilizeResponseMessage(predecessorHost,id);
        }
    };
}
