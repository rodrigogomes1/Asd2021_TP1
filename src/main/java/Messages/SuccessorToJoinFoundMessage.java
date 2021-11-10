package Messages;

import java.io.IOException;
import java.math.BigInteger;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

public class SuccessorToJoinFoundMessage extends ProtoMessage {
	public static final short MSG_ID = 202;

    
    private final Host successorHost;
    
    private final BigInteger successorId;

    //private final BigInteger hash;

    @Override
    public String toString() {
        return "SuccessorToJoinFoundMessage{" +
                "mid=" +
                '}';
    }

    public SuccessorToJoinFoundMessage( Host successorHost, BigInteger successorId) {
        super(MSG_ID);
        this.successorHost = successorHost;
        this.successorId= successorId;
        //this.hash = hash;
    }

    public Host getsuccessorHost() {
        return successorHost;
    }
    
    public BigInteger getsuccessorId() {
        return successorId;
    }


    public static ISerializer<SuccessorToJoinFoundMessage> serializer = new ISerializer<>() {
        @Override
        public void serialize(SuccessorToJoinFoundMessage finishJoinMessage, ByteBuf out) throws IOException {
            //out.writeLong(floodMessage.mid.getMostSignificantBits());
            //out.writeLong(floodMessage.mid.getLeastSignificantBits());
            Host.serializer.serialize(finishJoinMessage.successorHost, out);
            
        }

        @Override
        public SuccessorToJoinFoundMessage deserialize(ByteBuf in) throws IOException {
            Host successorHost = Host.serializer.deserialize(in);
            BigInteger id= BigInteger.valueOf(1);
          
        

            return new SuccessorToJoinFoundMessage(successorHost,id);
        }
    };
}

