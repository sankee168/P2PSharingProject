package networks.impl;

import java.util.Collection;

public interface PeerEvents {
    public void neighborsCompletedDownload();

    public void chokedPeers(Collection<Integer> chokedPeerIds);
    public void unchokedPeers(Collection<Integer> unchokedPeerIds);
}
