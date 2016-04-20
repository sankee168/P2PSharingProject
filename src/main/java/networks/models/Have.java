package networks.models;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by sank on 4/19/16.
 */
public class Have extends Message {
    public Have(byte[] partIndex) {
        super(MessageType.Have, partIndex);
    }

    public int getPartIndex() {
        return ByteBuffer.wrap(Arrays.copyOfRange(payload, 0, 4)).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    protected static byte[] getPartIndexBytes (int pieceIdx) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(pieceIdx).array();
    }
}
