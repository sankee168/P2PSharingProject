package impl;

import java.util.Collection;

/**
 * Created by sank on 4/20/16.
 */
//todo: change the variable names
    //todo: change the data structure for unchoked to integers
public interface PeerEvents {
    public void neighborsCompletedDownload();

    public void chockedPeers (Collection<Integer> chokedPeersIds);
    public void unchockedPeers (Collection<Integer> unchokedPeersIds);
}
