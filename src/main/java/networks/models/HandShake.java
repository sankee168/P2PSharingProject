package networks.models;

import lombok.Data;
import networks.references.Constants.HandShakeHeader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by sank on 4/19/16.
 */

//todo:
@Data
public class HandShake {
    private final byte[] zeroBits = new byte[HandShakeHeader.ZERO_BIT_SIZE];
    private final byte[] peerIdBits;

    public HandShake(){
        peerIdBits = new byte[HandShakeHeader.PEER_ID_BIT_SIZE];
    }

    public HandShake(byte[] peerId){
        if(peerId.length > HandShakeHeader.PEER_ID_BIT_SIZE){
            throw new IndexOutOfBoundsException("PeerId should have max length" + HandShakeHeader.PEER_ID_BIT_SIZE);
        }
        this.peerIdBits = peerId;
    }

    public void write(DataOutputStream outputStream) throws IOException {
        byte[] protocolId = HandShakeHeader.headerName.getBytes();

        outputStream.write (protocolId, 0, protocolId.length);
        outputStream.write(zeroBits, 0, zeroBits.length);
        outputStream.write(peerIdBits, 0, peerIdBits.length);
    }

    /*
    *   TODO: Create Custom Exception class and call it in these if-statements
    */
    public void read(DataInputStream inputStream) throws IOException {
        byte[] protocolId = new byte[HandShakeHeader.headerName.length()];
        if(inputStream.read(protocolId,0,protocolId.length) < protocolId.length){

        }
        if(inputStream.read(zeroBits, 0, zeroBits.length) < zeroBits.length){

        }

        if(inputStream.read(peerIdBits, 0, peerIdBits.length) < peerIdBits.length){

        }
    }
}
