package protocols.dht.messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class StoreSuccessMsg extends ProtoMessage {
    final public static short MSG_ID = 734;
    private String name;
    private UUID uid;

    public StoreSuccessMsg(String name, UUID uid) {
        super(MSG_ID);
        this.name = name;
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public UUID getUid() {
        return uid;
    }

    public static ISerializer< StoreSuccessMsg> serializer = new ISerializer< StoreSuccessMsg>() {
        @Override
        public void serialize( StoreSuccessMsg msg, ByteBuf out) throws IOException {
            out.writeInt(msg.getName().getBytes(StandardCharsets.UTF_8).length);
            out.writeBytes(msg.getName().getBytes(StandardCharsets.UTF_8));
            out.writeLong(msg.getUid().getMostSignificantBits());
            out.writeLong(msg.getUid().getLeastSignificantBits());
        }

        @Override
        public  StoreSuccessMsg deserialize(ByteBuf in) throws IOException {

            int nameLength = in.readInt();
            byte[] nameBytes = new byte[nameLength];
            in.readBytes(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8);

            long firstLong = in.readLong();
            long secondLong = in.readLong();
            UUID uid = new UUID(firstLong, secondLong);

            return new  StoreSuccessMsg(name, uid);
        }
    };
}
