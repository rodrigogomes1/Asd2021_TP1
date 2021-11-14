package protocols.dht.messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class ConfirmBulkStoreMsg extends ProtoMessage {
    final public static short MSG_ID = 413;
    Set<BigInteger> ids;

    public ConfirmBulkStoreMsg(Set<BigInteger> ids) {
        super(MSG_ID);
        this.ids = ids;
    }

    public Set<BigInteger> getFileIds() {
        return ids;
    }

    public static ISerializer< ConfirmBulkStoreMsg> serializer = new ISerializer< ConfirmBulkStoreMsg>() {
        @Override
        public void serialize( ConfirmBulkStoreMsg msg, ByteBuf out) throws IOException {
            int size = msg.getFileIds().size();
            out.writeInt(size);
            for(BigInteger id: msg.getFileIds()) {
                byte[] byteId =id.toByteArray();
                out.writeBytes(byteId);
            }

        }

        @Override
        public  ConfirmBulkStoreMsg deserialize(ByteBuf in) throws IOException {
            int size = in.readInt();
            Set<BigInteger> ids = new HashSet<>(size);
            for(int i = 0; i<size; i++){
                byte[] byteId = new byte[20];
                in.readBytes(byteId);
                BigInteger id = new BigInteger(byteId);
                ids.add(id);
            }

            return new  ConfirmBulkStoreMsg(ids);
        }
    };

}
