package protocols.dht.messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class LookupResponseMsg extends ProtoMessage {
    final public static short MSG_ID = 714;

    private String name;
    private byte[] content;
    private UUID uid;

    public LookupResponseMsg(String name, UUID uid, byte[] content) {
        super(MSG_ID);
        this.name = name;
        this.content = content;
        this.uid = uid;
    }
    public String getName() {
        return name;
    }

    public byte[] getContent() {
        return content;
    }

    public UUID getUid() {
        return uid;
    }


    public static ISerializer< LookupResponseMsg> serializer = new ISerializer< LookupResponseMsg>() {
        @Override
        public void serialize( LookupResponseMsg msg, ByteBuf out) throws IOException {
            //host, id, name, content, uid
            out.writeInt(msg.getName().getBytes(StandardCharsets.UTF_8).length);
            out.writeBytes(msg.getName().getBytes(StandardCharsets.UTF_8));
            out.writeInt(msg.getContent().length);
            out.writeBytes(msg.getContent());
            out.writeLong(msg.getUid().getMostSignificantBits());
            out.writeLong(msg.getUid().getLeastSignificantBits());
        }

        @Override
        public  LookupResponseMsg deserialize(ByteBuf in) throws IOException {
            int nameLength = in.readInt();
            byte[] nameBytes = new byte[nameLength];
            in.readBytes(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8);

            int contentLength = in.readInt();
            byte[] content = new byte[contentLength];
            in.readBytes(content);

            long firstLong = in.readLong();
            long secondLong = in.readLong();
            UUID uid = new UUID(firstLong, secondLong);

            return new  LookupResponseMsg(name, uid, content);
        }
    };
}

