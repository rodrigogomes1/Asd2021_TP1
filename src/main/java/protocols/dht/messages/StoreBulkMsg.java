package protocols.dht.messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class StoreBulkMsg extends ProtoMessage {
    final public static short MSG_ID = 777;
    Set<String> names;
    Set<byte[]> contents;

    public StoreBulkMsg(Set<String> names, Set<byte[]> contents) {
        super(MSG_ID);
        this.names = names;
        this.contents = contents;
    }

    public Set<String> getNames() {
        return names;
    }

    public Set<byte[]> getContents() {
        return contents;
    }

    public static ISerializer< StoreBulkMsg> serializer = new ISerializer< StoreBulkMsg>() {
        @Override
        public void serialize( StoreBulkMsg msg, ByteBuf out) throws IOException {
            int size = msg.getNames().size();
            out.writeInt(size);
            for(String name: msg.getNames()) {
                byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
                out.writeInt(nameBytes.length);
                out.writeBytes(nameBytes);
            }
            for(byte[] content: msg.getContents()) {
                out.writeInt(content.length);
                out.writeBytes(content);
            }
        }

        @Override
        public  StoreBulkMsg deserialize(ByteBuf in) throws IOException {
            int size = in.readInt();
            Set<String> names = new HashSet<>(size);
            Set<byte[]> contents = new HashSet<>(size);

            int nameLength;
            byte[] nameBytes;
            String name;
            for(int i = 0; i<size; i++){
                nameLength = in.readInt();
                nameBytes = new byte[nameLength];
                in.readBytes(nameBytes);
                name = new String(nameBytes, StandardCharsets.UTF_8);
                names.add(name);
            }

            int contentLength;
            byte[] content;
            for(int i = 0; i<size; i++){
                contentLength = in.readInt();
                content = new byte[contentLength];
                in.readBytes(content);
                contents.add(content);
            }
            return new  StoreBulkMsg(names, contents);
        }
    };
}
