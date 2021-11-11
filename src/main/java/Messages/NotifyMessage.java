package Messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.math.BigInteger;

public class NotifyMessage extends ProtoMessage {
    public static final short MSG_ID = 205;

    private final Host host;
    private final BigInteger id;

    @Override
    public String toString() {
        return "StabilizeResponseMessage{" +
                "mid=" +
                '}';
    }

    public NotifyMessage( Host host, BigInteger id) {
        super(MSG_ID);
        this.host = host;
        this.id= id;
        //this.hash = hash;
    }

    public Host getHost() {
        return host;
    }

    public BigInteger getNotifyId() {
        return id;
    }


    public static ISerializer<NotifyMessage> serializer = new ISerializer<>() {
        @Override
        public void serialize(NotifyMessage finishJoinMessage, ByteBuf out) throws IOException {
            //out.writeLong(floodMessage.mid.getMostSignificantBits());
            //out.writeLong(floodMessage.mid.getLeastSignificantBits());
            Host.serializer.serialize(finishJoinMessage.host, out);

        }

        @Override
        public NotifyMessage deserialize(ByteBuf in) throws IOException {
            Host host = Host.serializer.deserialize(in);
            BigInteger id= BigInteger.valueOf(1);



            return new NotifyMessage(host,id);
        }
    };
}
