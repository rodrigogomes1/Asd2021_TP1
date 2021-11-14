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

public class AskForContentMsg extends ProtoMessage {

    public final static short MSG_ID = 740;

    
    private final BigInteger hasId;

    public AskForContentMsg(BigInteger hasId) {
        super(MSG_ID);
        this.hasId = hasId;
    }

    

    public BigInteger getHashId() {
        return hasId;
    }
    
    

    @Override
    public String toString() {
        return "AskForContentMsg{" +
                '}';
    }

    public static ISerializer<AskForContentMsg> serializer = new ISerializer<AskForContentMsg>() {
        @Override
        public void serialize(AskForContentMsg msg, ByteBuf out) throws IOException {
            byte[] byteHashId = msg.getHashId().toByteArray();
            out.writeBytes(byteHashId);
        }

        @Override
        public AskForContentMsg deserialize(ByteBuf in) throws IOException {
            byte[] byteHashId = new byte[20];
            in.readBytes(byteHashId);
            BigInteger hashId = new BigInteger(byteHashId);
            return new AskForContentMsg(hashId);
        }
    };
}