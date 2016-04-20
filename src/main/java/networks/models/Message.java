package networks.models;

import lombok.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by sank on 4/19/16.
 */
@Data
public class Message {


    private int length;
    private final MessageType messageType;
    protected byte[] payload;

    protected Message (MessageType type) {
        this (type, null);
    }

    protected Message (MessageType messageType1, byte[] payload1) {
        length = (payload == null ? 0 : payload.length)
                + 1; // for the _type
        messageType = messageType1;
        this.payload = payload;
    }

    //getType is replaced by getMessageType
//    public Type getType() {
//        return _type;
//    }


    public void read (DataInputStream in) throws IOException {
        if ((payload != null) && (payload.length) > 0) {
            in.readFully(payload, 0, payload.length);
        }
    }


    public void write (DataOutputStream out) throws IOException {
        out.writeInt (length);
        out.writeByte (messageType.getMessageType());
        if ((payload != null) && (payload.length > 0)) {
            out.write (payload, 0, payload.length);
        }
    }

    public static Message getInstance (int length, MessageType type) throws ClassNotFoundException, IOException {
        switch (type) {
            case Choke:
                return new Choke();

            case Unchoke:
                return new Unchoke();

            case Interested:
                return new Interested();

            case NotInterested:
                return new NotInterested();

            case Have:
                return new Have (new byte[length]);

            case BitField:
                return new Bitfield (new byte[length]);

            case Request:
                return new Request (new byte[length]);

            case Piece:
                return new Piece (new byte[length]);

            default:
                throw new ClassNotFoundException ("message type not handled: " + type.toString());
        }
    }

}
