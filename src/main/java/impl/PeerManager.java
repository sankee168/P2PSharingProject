package impl;

import models.RemotePeerInfo;
import references.Constants;
import utilities.PropertyFileUtility;
import utilities.RandomUtils;

import java.rmi.Remote;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by sank on 4/20/16.
 */
//todo: need to edit this file
public class PeerManager implements Runnable{
    class OptimisticUnchoker extends Thread {
        private final int _numberOfOptimisticallyUnchokedNeighbors;
        private final int _optimisticUnchokingInterval;
        private final List<RemotePeerInfo> _chokedNeighbors = new ArrayList<RemotePeerInfo>();
        final Collection<RemotePeerInfo> _optmisticallyUnchokedPeers =
                Collections.newSetFromMap(new ConcurrentHashMap<RemotePeerInfo, Boolean>());

        OptimisticUnchoker(PropertyFileUtility conf) {
            super("OptimisticUnchoker");
            _numberOfOptimisticallyUnchokedNeighbors = 1;
            _optimisticUnchokingInterval = conf.getIntegerValue(Constants.CommonConfig.numberOfPreferredNeighbours) * 1000;
        }

        synchronized void setChokedNeighbors(Collection<RemotePeerInfo> chokedNeighbors) {
            _chokedNeighbors.clear();
            _chokedNeighbors.addAll(chokedNeighbors);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(_optimisticUnchokingInterval);
                } catch (InterruptedException ex) {
                }

                synchronized (this) {
                    // Randomly shuffle the remaining neighbors, and select some to optimistically unchoke
                    if (!_chokedNeighbors.isEmpty()) {
                        Collections.shuffle(_chokedNeighbors);
                        _optmisticallyUnchokedPeers.clear();
                        _optmisticallyUnchokedPeers.addAll(_chokedNeighbors.subList(0,
                                Math.min(_numberOfOptimisticallyUnchokedNeighbors, _chokedNeighbors.size())));
                    }
                }

                if (_chokedNeighbors.size() > 0) {
//                    LogHelper.getLogger().debug("STATE: OPT UNCHOKED(" + _numberOfOptimisticallyUnchokedNeighbors + "):" + LogHelper.getPeerIdsAsString (_optmisticallyUnchokedPeers));
//                    _eventLogger.changeOfOptimisticallyUnchokedNeighbors(LogHelper.getPeerIdsAsString (_optmisticallyUnchokedPeers));
                }
                for (PeerEvents listener : _listeners) {
                    listener.unchockedPeers(new RandomUtils().getIds(_optmisticallyUnchokedPeers));
                }
            }
        }
    }

    private final int _numberOfPreferredNeighbors;
    private final int _unchokingInterval;
    private final int _bitmapsize;
//    private final EventLogger _eventLogger;
    private final List<RemotePeerInfo> _peers = new ArrayList<RemotePeerInfo>();
    private final Collection<RemotePeerInfo> _preferredPeers = new HashSet<RemotePeerInfo>();
    private final OptimisticUnchoker _optUnchoker;
    private final Collection<PeerEvents> _listeners = new LinkedList<PeerEvents>();
    private final AtomicBoolean _randomlySelectPreferred = new AtomicBoolean(false);

    PeerManager(int peerId, Collection<RemotePeerInfo> peers, int bitmapsize, PropertyFileUtility conf) {
        _peers.addAll(peers);
        _numberOfPreferredNeighbors = Integer.parseInt(
                conf.getStringValue(Constants.CommonConfig.numberOfPreferredNeighbours));
        _unchokingInterval = Integer.parseInt(
                conf.getStringValue(Constants.CommonConfig.unChokingInterval)) * 1000;
        _optUnchoker = new OptimisticUnchoker(conf);
        _bitmapsize = bitmapsize;
//        _eventLogger = new EventLogger (peerId);
    }

    synchronized void addInterestPeer(int remotePeerId) {
        RemotePeerInfo peer = searchPeer(remotePeerId);
        if (peer != null) {
            peer.setInterested(true);
        }
    }

    long getUnchokingInterval() {
        return _unchokingInterval;
    }

    synchronized void removeInterestPeer(int remotePeerId) {
        RemotePeerInfo peer = searchPeer(remotePeerId);
        if (peer != null) {
            peer.setInterested(false);
        }
    }

    synchronized List<RemotePeerInfo> getInterestedPeers() {
        ArrayList<RemotePeerInfo> interestedPeers = new ArrayList<RemotePeerInfo>();
        for (RemotePeerInfo peer : _peers){
            if(peer.interested){
                interestedPeers.add(peer);
            }
        }
        return interestedPeers;
    }

    synchronized boolean isInteresting(int peerId, BitSet bitset) {
        RemotePeerInfo peer  = searchPeer(peerId);
        if (peer != null) {
            BitSet pBitset = (BitSet) peer.getReceivedParts().clone();
            pBitset.andNot(bitset);
            return ! pBitset.isEmpty();
        }
        return false;
    }

    synchronized void receivedPart(int peerId, int size) {
        RemotePeerInfo peer  = searchPeer(peerId);
        if (peer != null) {
            peer.getBytesDownloadedFrom().addAndGet(size);
        }
    }

    synchronized boolean canUploadToPeer(int peerId) {
        RemotePeerInfo peerInfo = new RemotePeerInfo(peerId);
        return (_preferredPeers.contains(peerInfo) ||
                _optUnchoker._optmisticallyUnchokedPeers.contains(peerInfo));
    }

    synchronized void fileCompleted() {
        _randomlySelectPreferred.set (true);
    }

    synchronized void bitfieldArrived(int peerId, BitSet bitfield) {
        RemotePeerInfo peer  = searchPeer(peerId);
        if (peer != null) {
            peer.setReceivedParts(bitfield);
        }
        neighborsCompletedDownload();
    }

    synchronized void haveArrived(int peerId, int partId) {
        RemotePeerInfo peer  = searchPeer(peerId);
        if (peer != null) {
            peer.getReceivedParts().set(partId);
        }
        neighborsCompletedDownload();
    }

    synchronized BitSet getReceivedParts(int peerId) {
        RemotePeerInfo peer  = searchPeer(peerId);
        if (peer != null) {
            return (BitSet) peer.getReceivedParts().clone();
        }
        return new BitSet();  // empry bit set
    }

    synchronized private RemotePeerInfo searchPeer(int peerId) {
        for (RemotePeerInfo peer : _peers) {
            if (peer.getPeerId() == peerId) {
                return peer;
            }
        }
//        LogHelper.getLogger().warning("Peer " + peerId + " not found");
        return null;
    }

    synchronized private void neighborsCompletedDownload() {
        for (RemotePeerInfo peer : _peers) {
            if (peer.receivedParts.cardinality() < _bitmapsize) {
                // at least one neighbor has not completed
//                LogHelper.getLogger().debug("Peer " + peer.getPeerId() + " has not completed yet");
                return;
            }
        }
        for (PeerEvents listener : _listeners) {
            listener.neighborsCompletedDownload();
        }
    }

    public synchronized void registerListener(PeerEvents listener) {
        _listeners.add(listener);
    }

    public void run() {

        _optUnchoker.start();

        while (true) {
            try {
                Thread.sleep(_unchokingInterval);
            } catch (InterruptedException ex) {
            }

            // 1) GET INTERESTED PEERS AND SORT THEM BY PREFERENCE

            List<RemotePeerInfo> interestedPeers = getInterestedPeers();
            if (_randomlySelectPreferred.get()) {
                // Randomly shuffle the neighbors
                System.out.println("asdjhgajshgdasjgdsajhgd");
//                LogHelper.getLogger().debug("selecting preferred peers randomly");
                Collections.shuffle(interestedPeers);
            }
            else {
                // Sort the peers in order of preference
                Collections.sort(interestedPeers, new Comparator() {
                    public int compare(Object o1, Object o2) {
                        RemotePeerInfo ri1 = (RemotePeerInfo) (o1);
                        RemotePeerInfo ri2 = (RemotePeerInfo) (o2);
                        // Sort in decreasing order
                        return (ri2.getBytesDownloadedFrom().get() - ri1.getBytesDownloadedFrom().get());
                    }
                });
            }

            Collection<RemotePeerInfo> optUnchokablePeers = null;

            Collection<Integer> chokedPeersIDs = new HashSet<Integer>();
            Collection<Integer> preferredNeighborsIDs = new HashSet<Integer>();
            Map<Integer, Long> downloadedBytes = new HashMap<Integer, Long>();

            synchronized (this) {
                // Reset downloaded bytes, but buffer them for debugging
                for (RemotePeerInfo peer : _peers) {
                    downloadedBytes.put (peer.getPeerId(), peer.getBytesDownloadedFrom().longValue());
                    peer.getBytesDownloadedFrom().set(0);
                }

                // 2) SELECT THE PREFERRED PEERS BY SELECTING THE HIGHEST RANKED

                // Select the highest ranked neighbors as "preferred"
                _preferredPeers.clear();
                _preferredPeers.addAll(interestedPeers.subList(0, Math.min(_numberOfPreferredNeighbors, interestedPeers.size())));
                if (_preferredPeers.size() > 0) {
//                    _eventLogger.changeOfPrefereedNeighbors(LogHelper.getPeerIdsAsString (_preferredPeers));
                    System.out.println("aksgdjashgdjkagmsdjasgjd");
                }

                // 3) SELECT ALLE THE INTERESTED AND UNINTERESTED PEERS, REMOVE THE PREFERRED. THE RESULTS ARE THE CHOKED PEERS

                Collection<RemotePeerInfo> chokedPeers = new LinkedList<RemotePeerInfo>(_peers);
                chokedPeers.removeAll(_preferredPeers);
                chokedPeersIDs.addAll(new RandomUtils().getIds(chokedPeers));

                // 4) SELECT ALLE THE INTERESTED PEERS, REMOVE THE PREFERRED. THE RESULTS ARE THE CHOKED PEERS THAT ARE "OPTIMISTICALLY-UNCHOKABLE"
                if (_numberOfPreferredNeighbors >= interestedPeers.size()) {
                    optUnchokablePeers = new ArrayList<RemotePeerInfo>();
                }
                else {
                    optUnchokablePeers = interestedPeers.subList(_numberOfPreferredNeighbors, interestedPeers.size());
                }

                preferredNeighborsIDs.addAll (new RandomUtils().getIds(_preferredPeers));
            }

            // debug
//            LogHelper.getLogger().debug("STATE: INTERESTED:" + LogHelper.getPeerIdsAsString (interestedPeers));
//            LogHelper.getLogger().debug("STATE: UNCHOKED (" + _numberOfPreferredNeighbors + "):" + LogHelper.getPeerIdsAsString2 (preferredNeighborsIDs));
//            LogHelper.getLogger().debug("STATE: CHOKED:" + LogHelper.getPeerIdsAsString2 (chokedPeersIDs));

            for (Map.Entry<Integer,Long> entry : downloadedBytes.entrySet()) {
                String PREFERRED = preferredNeighborsIDs.contains(entry.getKey()) ? " *" : "";
                System.out.println("askjdhgasjgdashdghaskjdgasjhdgajsghdjas");
//                LogHelper.getLogger().debug("BYTES DOWNLOADED FROM  PEER " + entry.getKey() + ": "
//                        + entry.getValue() + " (INTERESTED PEERS: "
//                        + interestedPeers.size()+ ": " + LogHelper.getPeerIdsAsString (interestedPeers)
//                        + ")\t" + PREFERRED);
            }

            // 5) NOTIFY PROCESS, IT WILL TAKE CARE OF SENDING CHOKE AND UNCHOKE MESSAGES

            for (PeerEvents listener : _listeners) {
                listener.chockedPeers(chokedPeersIDs);
                listener.unchockedPeers(preferredNeighborsIDs);
            }

            // 6) NOTIFY THE OPTIMISTICALLY UNCHOKER THREAD WITH THE NEW SET OF UNCHOKABLE PEERS

            if (optUnchokablePeers != null) {
                _optUnchoker.setChokedNeighbors(optUnchokablePeers);
            }
        }

    }
}
