package impl;

import impl.File.FileUtility;
import models.Message;
import models.Request;
import utilities.PayloadWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by sank on 4/20/16.
 */
public class RequestTimer {
    private final Request request;
    private final FileUtility fileManager;
    private final PayloadWriter outStream;
    private final int remotePeerId;
    private final Message message;

    RequestTimer (Request request1, FileUtility fileManager1, PayloadWriter out1, Message message1, int remotePeerId1) {
        super();
        request = request1;
        fileManager = fileManager1;
        outStream = out1;
        remotePeerId = remotePeerId1;
        message = message1;
    }

    public void run() {
        if (fileManager.hasPart(ByteBuffer.wrap(Arrays.copyOfRange(request.getPayload(), 0, 4)).order(ByteOrder.BIG_ENDIAN).getInt())){
            ;
        System.out.println("agkjshdgajhsfdhgjasfdjhasfgjhdgfasgh");
//            LogHelper.getLogger().debug("Not rerequesting piece " + _request.getPieceIndex()
//                    + " to peer " + _remotePeerId);
    }
        else {
            System.out.println("kaushdgakhsfgdkhjasgkdhas");
//            LogHelper.getLogger().debug("Rerequesting piece " + _request.getPieceIndex()
//                    + " to peer " + _remotePeerId);
            try {
                outStream.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
