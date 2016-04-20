package networks.models;

import java.util.BitSet;

/**
 * Created by sank on 4/19/16.
 */
public class Bitfield extends Message {

    Bitfield(byte[] bitfield) {
        super(MessageType.BitField.BitField, bitfield);
    }

    public Bitfield(BitSet bitset) {
        super(MessageType.BitField, bitset.toByteArray());
    }

    public BitSet getBitSet() {
        return BitSet.valueOf(payload);
    }
}
