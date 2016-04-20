package networks.impl;

import networks.models.RemotePeerInfo;
import networks.utilities.LogHelper;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by sank on 4/20/16.
 */
public class StartRemotePeers {
    public static void main(String[] args) {
        PeerImplementation peerImplementation = new PeerImplementation();
        List<RemotePeerInfo> allPeers = peerImplementation.getAllPeerInfo();
        Iterator<RemotePeerInfo> iter = allPeers.iterator();
        while (iter.hasNext()) {
            RemotePeerInfo currPeer = iter.next();
            //todo: should I send user.dir here to startPeer or directly call?
            startPeer(currPeer);
        }
        LogHelper.getLogger().info("All peers have started");
    }

    public static void startPeer(RemotePeerInfo remotePeer) {
        String currPath = System.getProperty("user.dir");
        try {
            Runtime.getRuntime().exec ("ssh " + remotePeer.peerAddress + " cd " + currPath + "; java networks.peerProcess " + remotePeer.peerId);
        } catch (IOException e) {
            LogHelper.getLogger().warning(e);
        }

    }
}
