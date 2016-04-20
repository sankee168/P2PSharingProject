package networks.models;

import lombok.Data;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class RemotePeerInfo {

    public int peerId;
    public String peerAddress;
    public int peerPort;
    public final boolean hasFile;
    public AtomicInteger bytesDownloadedFrom;
    public BitSet receivedParts;
    public Boolean interested;

    public RemotePeerInfo (int peerId) {
        this (peerId, "127.0.0.1", 0, false);
    }

    public RemotePeerInfo(int pId, String pAddress, int pPort, boolean hasFile1) {
        peerId = pId;
        peerAddress = pAddress;
        peerPort = pPort;
        hasFile = hasFile1;
        bytesDownloadedFrom = new AtomicInteger (0);
        receivedParts = new BitSet();
        interested = false;
    }

    @Override
    public boolean equals (Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof RemotePeerInfo) {
            return (((RemotePeerInfo) obj).peerId == (peerId));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.peerId);
        return hash;
    }

    @Override
    public String toString() {
        return new StringBuilder (peerId)
                .append (" address:").append (peerAddress)
                .append(" port: ").append(peerPort).toString();
    }
}
