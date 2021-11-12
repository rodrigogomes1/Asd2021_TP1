package protocols.dht.messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class LookupMsg extends ProtoMessage {
    final public static short MSG_ID = 371;

    private Host host;
    private BigInteger fileId;
    private String name;
    private UUID uid;

    public LookupMsg(Host host, BigInteger fileId, String name, UUID uid) {
        super(MSG_ID);
        this.host = host;
        this.fileId = fileId;
        this.name = name;
        this.uid = uid;
    }

    public Host getHost() {
        return host;
    }

    public BigInteger getFileId() {
        return fileId;
    }

    public String getName() {
        return name;
    }

    public UUID getUid() {
        return uid;
    }

    public static ISerializer<LookupMsg> serializer = new ISerializer< LookupMsg>() {
        @Override
        public void serialize( LookupMsg msg, ByteBuf out) throws IOException {
            Host.serializer.serialize(msg.getHost(), out);
            byte[] byteId = msg.getFileId().toByteArray();
            out.writeBytes(byteId);
            out.writeInt(msg.getName().getBytes(StandardCharsets.UTF_8).length);
            out.writeBytes(msg.getName().getBytes(StandardCharsets.UTF_8));
            out.writeLong(msg.getUid().getMostSignificantBits());
            out.writeLong(msg.getUid().getLeastSignificantBits());
        }

        @Override
        public LookupMsg deserialize(ByteBuf in) throws IOException {
            Host host = Host.serializer.deserialize(in);
            byte[] byteId = new byte[20];
            in.readBytes(byteId);
            BigInteger fileId = new BigInteger(byteId);

            int nameLength = in.readInt();
            byte[] nameBytes = new byte[nameLength];
            in.readBytes(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8);

            long firstLong = in.readLong();
            long secondLong = in.readLong();
            UUID uid = new UUID(firstLong, secondLong);

            return new  LookupMsg(host, fileId, name, uid);
        }
    };
}
