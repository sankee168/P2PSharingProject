import config.PeerInfo;
import impl.PeerImplementation;
import models.RemotePeerInfo;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * Created by sank on 4/20/16.
 */
public class peerProcess {
    public static void main (String[] args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        PeerImplementation peerImplementation = new PeerImplementation();
        if (args.length != 1) {
            //todo: log
            System.out.println("check here 1");
//            LogHelper.getLogger().severe("the number of arguments passed to the program is " + args.length + " while it should be 1.\nUsage: java peerProcess peerId");
        }
        final int peerId = Integer.parseInt(args[0]);
        //todo: configure logger here
//        LogHelper.configure(peerId);
        String address = "localhost";
        int port = 6008;
        boolean hasFile = false;

        // Read properties
        Reader commReader = null;
        Reader peerReader = null;
        Properties commProp = null;
        PeerInfo peerInfo = new PeerInfo();
        List<RemotePeerInfo> peersToConnect = new LinkedList<RemotePeerInfo>();
        try {
//            commReader = new FileReader(CommonProperties.CONFIG_FILE_NAME);
//            peerReader = new FileReader (PeerInfo.CONFIG_FILE_NAME);
//            commProp = CommonProperties.read (commReader);
//            peerInfo.read (peerReader);
            List<RemotePeerInfo> allPeers = peerImplementation.getAllPeerInfo();
            Iterator<RemotePeerInfo> iter = allPeers.iterator();
            while (iter.hasNext()) {
                RemotePeerInfo currPeer = iter.next();
                if (peerId == currPeer.getPeerId()) {
                    address = currPeer.getPeerAddress();
                    port = currPeer.getPeerPort();
                    hasFile = currPeer.isHasFile();
                    //todo: remove below lines
                    // A peer connects only to the previously defined peers,
                    // therefore I can stop parsing here.
                    break;
                } else {
                    peersToConnect.add(currPeer);
                    System.out.println("Read configuration for peer " + currPeer.getPeerId());
//                    LogHelper.getLogger().conf ("Read configuration for peer: " + peer);
                }
            }
//        }

//            for(int i = 0; i < allPeers.size();i ++) {
//                RemotePeerInfo currPeer = allPeers[i];
//                if (peerId == peer.getPeerId()) {
//                    address = peer.getPeerAddress();
//                    port = peer.getPort();
//                    hasFile = peer.hasFile();
//                    // A peer connects only to the previously defined peers,
//                    // therefore I can stop parsing here.
//                    break;
//                }
////            }
//            for (RemotePeerInfo peer : peerInfo.getPeerInfo()) {
//                if (peerId == peer.getPeerId()) {
//                    address = peer.getPeerAddress();
//                    port = peer.getPort();
//                    hasFile = peer.hasFile();
//                    // A peer connects only to the previously defined peers,
//                    // therefore I can stop parsing here.
//                    break;
//                }
//                else {
//                    peersToConnectTo.add (peer);
//                    LogHelper.getLogger().conf ("Read configuration for peer: " + peer);
//                }
//            }


//        Process peerProc = new Process (peerId, address, port, hasFile, peerInfo.getPeerInfo(), commProp);
//        peerProc.init();
//        Thread t = new Thread (peerProc);
//        t.setName ("peerProcess-" + peerId);
//        t.start();
//
//        //todo: log this
////        LogHelper.getLogger().debug ("Connecting to " + peersToConnectTo.size() + " peers.");
//        System.out.println("Connecting to " + peersToConnect.size() + " peers.");
//        peerProc.connectToPeers (peersToConnect);
//        try {
//            Thread.sleep(5);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

