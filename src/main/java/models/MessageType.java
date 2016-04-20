package models;


/**
 * Created by sank on 4/19/16.
 */
public enum MessageType {
    Choke((byte) 0),
    Unchoke((byte) 1),
    Interested((byte) 2),
    NotInterested((byte) 3),
    Have((byte) 4),
    BitField((byte) 5),
    Request((byte) 6),
    Piece((byte) 7);

    public static MessageType valueOf(byte inputByte) {
        for (MessageType messageType1 : MessageType.values()) {
            if (messageType1.messageType == inputByte) {
                return messageType1;
            }
        }
        throw new IllegalArgumentException();
    }

    private final byte messageType;

    MessageType(byte type) {
        messageType = type;
    }

    public byte getMessageType() {
        return this.messageType;
    }

//    getValue is replaced with getMessageType
//    public byte getValue() {
//        return messageType;
//    }




}
