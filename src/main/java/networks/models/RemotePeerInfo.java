package networks.models;

import lombok.Data;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sank on 4/19/16.
 */

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

//    @Override
//    public boolean equals (Object obj) {
//        if (obj == null) {
//            return false;
//        }
//        if (obj instanceof RemotePeerInfo) {
//            return (((RemotePeerInfo) obj).peerId.equals (peerId));
//        }
//        return false;
//    }

//    @Override
//    public int hashCode() {
//        int hash = 3;
//        hash = 97 * hash + Objects.hashCode(this._peerId);
//        return hash;
//    }
//
//    @Override
//    public String toString() {
//        return new StringBuilder (_peerId)
//                .append (" address:").append (_peerAddress)
//                .append(" port: ").append(_peerPort).toString();
//    }

//    public static Collection<Integer> toIdSet (Collection<RemotePeerInfo> peers) {
//        Set<Integer> ids = new HashSet<>();
//        for (RemotePeerInfo peer : peers) {
//            ids.add(peer.getPeerId());
//        }
//        return ids;
//    }
}
