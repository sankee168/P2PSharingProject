package networks.models;

import lombok.Data;
import networks.references.Constants.HandShakeHeader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by sank on 4/19/16.
 */

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

    public HandShake (int peerId) {

        this (ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(peerId).array());
        System.out.println("Peer id in out handshake " + peerId);
    }

    public void write(DataOutputStream outputStream) throws IOException {
        byte[] protocolId = HandShakeHeader.headerName.getBytes();

        outputStream.write (protocolId, 0, protocolId.length);
        outputStream.write(zeroBits, 0, zeroBits.length);
        outputStream.write(peerIdBits, 0, peerIdBits.length);
    }

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
