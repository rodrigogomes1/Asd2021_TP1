package Messages;

import java.io.IOException;
import java.math.BigInteger;
import java.util.UUID;

import com.sun.nio.sctp.PeerAddressChangeNotification;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

public class JoinMessage extends ProtoMessage {
    public static final short MSG_ID = 201;

    
    private final Host originalSender;
    
    private final BigInteger id;

    //private final BigInteger hash;

    @Override
    public String toString() {
        return "JoinMessage{" +
                "mid=" +
                '}';
    }

    public JoinMessage( Host originalSender, BigInteger peerId) {
        super(MSG_ID);
        this.originalSender = originalSender;
        this.id= peerId;
        //this.hash = hash;
    }

    public Host getOriginalSender() {
        return originalSender;
    }
    
    public BigInteger getpeerId() {
        return id;
    }


    public static ISerializer<JoinMessage> serializer = new ISerializer<>() {
        @Override
        public void serialize(JoinMessage joinMessage, ByteBuf out) throws IOException {
            //out.writeLong(floodMessage.mid.getMostSignificantBits());
            //out.writeLong(floodMessage.mid.getLeastSignificantBits());
            Host.serializer.serialize(joinMessage.originalSender, out);
            
        }

        @Override
        public JoinMessage deserialize(ByteBuf in) throws IOException {
            Host originalSender = Host.serializer.deserialize(in);
            BigInteger id= BigInteger.valueOf(1);
          
        

            return new JoinMessage(originalSender,id);
        }
    };
}
