package networks.impl;

import lombok.Data;
import networks.models.RemotePeerInfo;
import networks.references.Constants;
import networks.utilities.EventLogger;
import networks.utilities.LogHelper;
import networks.utilities.PropertyFileUtility;
import networks.utilities.RandomUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class PeerManager implements Runnable {
    class ChokeUtil extends Thread {
        private final int numberOfOptimisticallyUnchokedNeighbors;
        private final int optimisticUnchokingInterval;
        private final List<RemotePeerInfo> chokedNeighbors = new ArrayList<RemotePeerInfo>();
        final Collection<RemotePeerInfo> optmisticallyUnchokedPeers =
                Collections.newSetFromMap(new ConcurrentHashMap<RemotePeerInfo, Boolean>());

        ChokeUtil(PropertyFileUtility conf) {
            super("ChokeUtil");
            numberOfOptimisticallyUnchokedNeighbors = 1;
            optimisticUnchokingInterval = conf.getIntegerValue(Constants.CommonConfig.numberOfPreferredNeighbours) * Constants.Random.chokingIntervalFactor;
        }

        synchronized void setChokedNeighbors(Collection<RemotePeerInfo> chokedNeighbors) {
            chokedNeighbors.clear();
            chokedNeighbors.addAll(chokedNeighbors);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(optimisticUnchokingInterval);
                } catch (InterruptedException ex) {
                    LogHelper.getLogger().warning("Interrupted Exception Occurred");
                }

                synchronized (this) {
                    // Randomly shuffle the remaining neighbors, and select some to optimistically unchoke
                    if (!chokedNeighbors.isEmpty()) {
                        Collections.shuffle(chokedNeighbors);
                        optmisticallyUnchokedPeers.clear();
                        optmisticallyUnchokedPeers.addAll(chokedNeighbors.subList(0,
                                Math.min(numberOfOptimisticallyUnchokedNeighbors, chokedNeighbors.size())));
                    }
                }

                if (chokedNeighbors.size() > 0) {
                    LogHelper.getLogger().debug("STATE: OPT UNCHOKED(" + numberOfOptimisticallyUnchokedNeighbors + "):" + LogHelper.getPeerIdsAsString(optmisticallyUnchokedPeers));
                    eventLogger.changeOfOptimisticallyUnchokedNeighbors(LogHelper.getPeerIdsAsString(optmisticallyUnchokedPeers));
                }
                for (PeerEvents listener : listeners) {
                    listener.unchokedPeers(new RandomUtils().getIds(optmisticallyUnchokedPeers));
                }
            }
        }
    }

    private final int countOfPrefNeighbors;
    private final int unchokingInterval;
    private final int bitmapsize;
    private final EventLogger eventLogger;
    private final List<RemotePeerInfo> peers = new ArrayList();
    private final Collection<RemotePeerInfo> preferredPeers = new HashSet<RemotePeerInfo>();
    private final ChokeUtil optUnchoker;
    private final Collection<PeerEvents> listeners = new LinkedList<PeerEvents>();
    private final AtomicBoolean randomlySelectPreferred = new AtomicBoolean(false);

    PeerManager(int peerId, Collection<RemotePeerInfo> peers, int bitmapsize, PropertyFileUtility conf) {
        this.peers.addAll(peers);
        countOfPrefNeighbors = Integer.parseInt(
                conf.getStringValue(Constants.CommonConfig.numberOfPreferredNeighbours));
        unchokingInterval = Integer.parseInt(
                conf.getStringValue(Constants.CommonConfig.unChokingInterval)) * 1000;
        optUnchoker = new ChokeUtil(conf);
        this.bitmapsize = bitmapsize;
        eventLogger = new EventLogger(peerId);
    }

    synchronized void addInterestPeer(int remotePeerId) {
        RemotePeerInfo peer = searchPeer(remotePeerId);
        if (peer != null) {
            peer.setInterested(true);
        }
    }

    synchronized void removeInterestPeer(int remotePeerId) {
        RemotePeerInfo peer = searchPeer(remotePeerId);
        if (peer != null) {
            peer.setInterested(false);
        }
    }

    synchronized List<RemotePeerInfo> getInterestedPeers() {
        ArrayList<RemotePeerInfo> interestedPeers = new ArrayList<RemotePeerInfo>();
        for (RemotePeerInfo peer : peers) {
            if (peer.interested) {
                interestedPeers.add(peer);
            }
        }
        return interestedPeers;
    }

    synchronized boolean isInteresting(int peerId, BitSet bitset) {
        RemotePeerInfo peer = searchPeer(peerId);
        if (peer != null) {
            BitSet pBitset = (BitSet) peer.getReceivedParts().clone();
            pBitset.andNot(bitset);
            return !pBitset.isEmpty();
        }
        return false;
    }

    synchronized void receivedPart(int peerId, int size) {
        RemotePeerInfo peer = searchPeer(peerId);
        if (peer != null) {
            peer.getBytesDownloadedFrom().addAndGet(size);
        }
    }

    synchronized boolean canUploadToPeer(int peerId) {
        RemotePeerInfo peerInfo = new RemotePeerInfo(peerId);
        return (preferredPeers.contains(peerInfo) ||
                optUnchoker.optmisticallyUnchokedPeers.contains(peerInfo));
    }

    synchronized void fileCompleted() {
        randomlySelectPreferred.set(true);
    }

    synchronized void bitfieldArrived(int peerId, BitSet bitfield) {
        RemotePeerInfo peer = searchPeer(peerId);
        if (peer != null) {
            peer.setReceivedParts(bitfield);
        }
        neighborsCompletedDownload();
    }

    synchronized void haveArrived(int peerId, int partId) {
        RemotePeerInfo peer = searchPeer(peerId);
        if (peer != null) {
            peer.getReceivedParts().set(partId);
        }
        neighborsCompletedDownload();
    }

    synchronized BitSet getReceivedParts(int peerId) {
        RemotePeerInfo peer = searchPeer(peerId);
        if (peer != null) {
            return (BitSet) peer.getReceivedParts().clone();
        }
        return new BitSet();
    }

    synchronized private RemotePeerInfo searchPeer(int peerId) {
        for (RemotePeerInfo peer : peers) {
            if (peer.getPeerId() == peerId) {
                return peer;
            }
        }
        LogHelper.getLogger().warning("Peer " + peerId + " not found");
        return null;
    }

    synchronized private void neighborsCompletedDownload() {
        for (int i = 0; i < peers.size(); i++) {
            if (peers.get(i).receivedParts.cardinality() < bitmapsize) {
                LogHelper.getLogger().debug("Peer " + peers.get(i).getPeerId() + " has not completed yet");
                return;
            }
        }

        Iterator<PeerEvents> iter = listeners.iterator();

        while (iter.hasNext()) {
            PeerEvents listener = iter.next();
            listener.neighborsCompletedDownload();
        }
    }

    public synchronized void registerListener(PeerEvents listener) {
        listeners.add(listener);
    }

    public void run() {

        optUnchoker.start();

        while (true) {
            try {
                Thread.sleep(unchokingInterval);
            } catch (InterruptedException ex) {
            }

            List<RemotePeerInfo> interestedPeers = getInterestedPeers();
            if (randomlySelectPreferred.get()) {
                LogHelper.getLogger().debug("selecting preferred peers randomly");
                Collections.shuffle(interestedPeers);
            } else {
                Collections.sort(interestedPeers, new Comparator() {
                    public int compare(Object toObject, Object fromObject) {
                        RemotePeerInfo remotePeerInfo1 = (RemotePeerInfo) (toObject);
                        RemotePeerInfo remotePeerInfo2 = (RemotePeerInfo) (fromObject);
                        // Sort in decreasing order
                        return (remotePeerInfo2.getBytesDownloadedFrom().get() - remotePeerInfo1.getBytesDownloadedFrom().get());
                    }
                });
            }

            Collection<RemotePeerInfo> optUnchokablePeers = null;
            Collection<Integer> chokedPeersIDs = new HashSet<Integer>();
            Collection<Integer> preferredNeighborsIDs = new HashSet<Integer>();
            Map<Integer, Long> downloadedBytes = new HashMap<Integer, Long>();

            synchronized (this) {
                for (int i = 0; i < peers.size(); i++) {
                    RemotePeerInfo currPeer = peers.get(i);
                    downloadedBytes.put(currPeer.getPeerId(), currPeer.getBytesDownloadedFrom().longValue());
                    currPeer.getBytesDownloadedFrom().set(0);
                }
                preferredPeers.clear();
                preferredPeers.addAll(interestedPeers.subList(0, Math.min(countOfPrefNeighbors, interestedPeers.size())));
                if (preferredPeers.size() > 0) {
                    eventLogger.changeOfPrefereedNeighbors(LogHelper.getPeerIdsAsString(preferredPeers));
                }
                Collection<RemotePeerInfo> chokedPeers = new LinkedList<RemotePeerInfo>(peers);
                chokedPeers.removeAll(preferredPeers);
                chokedPeersIDs.addAll(new RandomUtils().getIds(chokedPeers));

                if (countOfPrefNeighbors >= interestedPeers.size()) {
                    optUnchokablePeers = new ArrayList<RemotePeerInfo>();
                } else {
                    optUnchokablePeers = interestedPeers.subList(countOfPrefNeighbors, interestedPeers.size());
                }

                preferredNeighborsIDs.addAll(new RandomUtils().getIds(preferredPeers));
            }
            LogHelper.getLogger().debug("STATE: INTERESTED:" + LogHelper.getPeerIdsAsString(interestedPeers));
            LogHelper.getLogger().debug("STATE: UNCHOKED (" + countOfPrefNeighbors + "):" + LogHelper.getPeerIdsAsString2(preferredNeighborsIDs));
            LogHelper.getLogger().debug("STATE: CHOKED:" + LogHelper.getPeerIdsAsString2(chokedPeersIDs));

            for (Map.Entry<Integer, Long> entry : downloadedBytes.entrySet()) {
                String PREFERRED = preferredNeighborsIDs.contains(entry.getKey()) ? " *" : "";
                LogHelper.getLogger().debug("BYTES DOWNLOADED FROM  PEER " + entry.getKey() + ": " + entry.getValue() + " (INTERESTED PEERS: " + interestedPeers.size() + ": " + LogHelper.getPeerIdsAsString(interestedPeers) + ")\t" + PREFERRED);
            }

            Iterator<PeerEvents> iter = listeners.iterator();
            while (iter.hasNext()) {
                PeerEvents listener = iter.next();
                listener.chokedPeers(chokedPeersIDs);
                listener.unchokedPeers(preferredNeighborsIDs);
            }

            if (optUnchokablePeers != null) {
                optUnchoker.setChokedNeighbors(optUnchokablePeers);
            }
        }

    }
}
