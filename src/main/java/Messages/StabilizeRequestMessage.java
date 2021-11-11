package Messages;

import java.io.IOException;
import java.math.BigInteger;
import java.util.UUID;

import com.sun.nio.sctp.PeerAddressChangeNotification;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

public class StabilizeRequestMessage extends ProtoMessage {
    public static final short MSG_ID = 205;

    
    private final Host sender;
    

    //private final BigInteger hash;

    @Override
    public String toString() {
        return "StabilizeRequestMessage{" +
                "mid=" +
                '}';
    }

    public StabilizeRequestMessage( Host sender) {
        super(MSG_ID);
        this.sender = sender;
        //this.hash = hash;
    }

    public Host getSender() {
        return sender;
    }
    
    


    public static ISerializer<StabilizeRequestMessage> serializer = new ISerializer<>() {
        @Override
        public void serialize(StabilizeRequestMessage stabilizeMessage, ByteBuf out) throws IOException {
            //out.writeLong(floodMessage.mid.getMostSignificantBits());
            //out.writeLong(floodMessage.mid.getLeastSignificantBits());
            Host.serializer.serialize(stabilizeMessage.sender, out);
            
        }

        @Override
        public StabilizeRequestMessage deserialize(ByteBuf in) throws IOException {
            Host sender = Host.serializer.deserialize(in);
          
        
            return new StabilizeRequestMessage(sender);
        }
    };
}
