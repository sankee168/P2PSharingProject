package networks.models;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by sank on 4/19/16.
 */
public class Piece extends Message {

    Piece (byte[] payload) {
        super (MessageType.Piece, payload);
    }

    public Piece (int indexOfPeice, byte[] content) {
        super (MessageType.Piece, join (indexOfPeice, content));
    }

    public byte[] getContent() {
        if ((payload == null) || (payload.length <= 4)) {
            return null;
        }
        return Arrays.copyOfRange(payload, 4, payload.length);
    }

    private static byte[] getSizeOfIndex(int partIndex) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(partIndex).array();
    }

    private static byte[] join (int indexOfPiece, byte[] second) {
        byte[] concat = new byte[4 + (second == null ? 0 : second.length)];
        System.arraycopy(getSizeOfIndex (indexOfPiece), 0, concat, 0, 4);
        System.arraycopy(second, 0, concat, 4, second.length);
        return concat;
    }

}
