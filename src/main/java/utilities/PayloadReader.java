package utilities;


import models.HandShake;
import models.Message;
import models.MessageType;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;

/**
 * Created by mallem on 4/20/16.
 */
public class PayloadReader extends DataInputStream implements ObjectInput {

    private boolean isHandShakeDone = false;

    public PayloadReader(InputStream in) {
        super(in);
    }

    public Object readObject() throws ClassNotFoundException, IOException {
        if(isHandShakeDone){
            int payloadLength = readInt() - 1;
            Message message = Message.getInstance(payloadLength, MessageType.valueOf(readByte()));
            message.read(this);
            return message;
        }
        else {
            HandShake handshake = new HandShake();
            handshake.read(this);
            isHandShakeDone = true;
            return handshake;
        }
    }
}
