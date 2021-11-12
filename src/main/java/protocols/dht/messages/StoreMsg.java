package protocols.dht.messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class StoreMsg extends ProtoMessage {
    final public static short MSG_ID = 735;

    private Host host;
    private BigInteger fileId;
    private String name;
    private byte[] content;
    private UUID uid;

    public StoreMsg(Host host, BigInteger fileId, String name, byte[] content, UUID uid) {
        super(MSG_ID);
        this.host = host;
        this.fileId = fileId;
        this.name = name;
        this.content = content;
        this.uid = uid;
    }

    public Host getHost() {
        return host;
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

    public BigInteger getFileId() {
        return fileId;
    }

    @Override
    public String toString() {
        return "StoreMessage{" +
                "subset=" + host +
                '}';
    }

    public static ISerializer< StoreMsg> serializer = new ISerializer< StoreMsg>() {
        @Override
        public void serialize( StoreMsg msg, ByteBuf out) throws IOException {
            Host.serializer.serialize(msg.getHost(), out);
            byte[] byteId = msg.getFileId().toByteArray();
            out.writeBytes(byteId);
            //host, id, name, content, uid
            out.writeInt(msg.getName().getBytes(StandardCharsets.UTF_8).length);
            out.writeBytes(msg.getName().getBytes(StandardCharsets.UTF_8));
            out.writeInt(msg.getContent().length);
            out.writeBytes(msg.getContent());
            out.writeLong(msg.getUid().getMostSignificantBits());
            out.writeLong(msg.getUid().getLeastSignificantBits());
        }

        @Override
        public  StoreMsg deserialize(ByteBuf in) throws IOException {
            Host host = Host.serializer.deserialize(in);
            byte[] byteId = new byte[20];
            in.readBytes(byteId);
            BigInteger fileId = new BigInteger(byteId);

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

            return new  StoreMsg(host, fileId, name, content, uid);
        }
    };
}
