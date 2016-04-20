package networks.impl;

import networks.impl.File.FileEvent;
import networks.impl.File.FileUtility;
import networks.models.*;
import networks.utilities.EventLogger;
import networks.utilities.LogHelper;
import networks.utilities.PropertyFileUtility;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Process implements Runnable, FileEvent, PeerEvents{
    private final int peerId;
    private final int port;
    private final boolean hasFile;
    private final PropertyFileUtility conf;
    private final FileUtility fileUtil;
    private final PeerManager peerMgr;
    private final EventLogger eventLogger;
    private final AtomicBoolean fileCompleted = new AtomicBoolean(false);
    private final AtomicBoolean peersFileCompleted = new AtomicBoolean(false);
    private final AtomicBoolean terminate = new AtomicBoolean(false);
    private final Collection<ConnectionManager> connHandlers =
            Collections.newSetFromMap(new ConcurrentHashMap<ConnectionManager, Boolean>());

    public Process(int peerId, String address, int port, boolean hasFile, Collection<RemotePeerInfo> peerInfo, PropertyFileUtility conf) {
        this.peerId = peerId;
        this.port = port;
        this.hasFile = hasFile;
        this.conf = conf;
        fileUtil = new FileUtility(this.peerId, this.conf);
        ArrayList<RemotePeerInfo> remotePeers = new ArrayList<RemotePeerInfo>(peerInfo);
        for (RemotePeerInfo ri : remotePeers) {
            if (ri.getPeerId() == peerId) {
                remotePeers.remove(ri);
                break;
            }
        }
        peerMgr = new PeerManager(this.peerId, remotePeers, fileUtil.getBitmapSize(), this.conf);
        eventLogger = new EventLogger(peerId);
        fileCompleted.set(this.hasFile);
    }

    public void init() {
        fileUtil.registerEvent(this);
        peerMgr.registerListener(this);

        if (hasFile) {
            LogHelper.getLogger().debug("Spltting file");
            fileUtil.splitFile();
            fileUtil.setAllChunks();
        }
        else {
            LogHelper.getLogger().debug("Peer does not have file " + peerId);
        }

        Thread t = new Thread(peerMgr);
        t.setName(peerMgr.getClass().getName());
        t.start();
    }

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (!terminate.get()) {
                try {
                    LogHelper.getLogger().debug(Thread.currentThread().getName() + ": Peer " + peerId + " listening on port " + port + ".");
                    addConnHandler(new ConnectionManager(peerId, serverSocket.accept(), fileUtil, peerMgr));

                } catch (Exception e) {
                    LogHelper.getLogger().warning(e);
                }
            }
        } catch (IOException ex) {
            LogHelper.getLogger().warning(ex);
        } finally {
            LogHelper.getLogger().warning(Thread.currentThread().getName()
                    + " terminating, TCP connections will no longer be accepted.");
        }
    }

    public void connectToPeers(Collection<RemotePeerInfo> peersToConnectTo) {
        Iterator<RemotePeerInfo> iter = peersToConnectTo.iterator();
        while (iter.hasNext()) {
            do {
                Socket socket = null;
                RemotePeerInfo peer = iter.next();
                try {
                    LogHelper.getLogger().debug(" Connecting to peer: " + peer.getPeerId()
                            + " (" + peer.getPeerAddress() + ":" + peer.getPeerPort() + ")");
                    socket = new Socket(peer.getPeerAddress(), peer.getPeerPort());
                    if (addConnHandler(new ConnectionManager(peerId, true, peer.getPeerId(),
                            socket, fileUtil, peerMgr))) {
                        iter.remove();
                        LogHelper.getLogger().debug(" Connected to peer: " + peer.getPeerId()
                                + " (" + peer.getPeerAddress() + ":" + peer.getPeerPort() + ")");

                    }
                }
                catch (ConnectException ex) {
                    LogHelper.getLogger().warning("could not connect to peer " + peer.getPeerId()
                            + " at address " + peer.getPeerAddress() + ":" + peer.getPeerPort());
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException ex1)
                        {}
                    }
                }
                catch (IOException ex) {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException ex1)
                        {}
                    }
                    LogHelper.getLogger().warning(ex);
                }
            }
            while (iter.hasNext());

            iter = peersToConnectTo.iterator();
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
            }
        }
    }

    public void neighborsCompletedDownload() {
        LogHelper.getLogger().debug("all peers completed download");
        peersFileCompleted.set(true);
        if (fileCompleted.get() && peersFileCompleted.get()) {
            // The process can quit
            terminate.set(true);
            System.exit(0);
        }
    }

    public synchronized void fileCompleted() {
        LogHelper.getLogger().debug("local peer completed download");
        eventLogger.fileDownloadedMessage();
        fileCompleted.set(true);
        if (fileCompleted.get() && peersFileCompleted.get()) {
            // The process can quit
            terminate.set(true);
            System.exit(0);
        }
    }

    public synchronized void pieceArrived(int partIdx) {
        for (ConnectionManager connHanlder : connHandlers) {
            connHanlder.send(new Have(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(partIdx).array()));
            if (!peerMgr.isInteresting(connHanlder.getRemotePeerId(), fileUtil.getReceivedChunks())) {
                connHanlder.send(new NotInterested());
            }
        }
    }

    private synchronized boolean addConnHandler(ConnectionManager connHandler) {
        if (!connHandlers.contains(connHandler)) {
            connHandlers.add(connHandler);
            new Thread(connHandler).start();
            try {
                wait(10);
            } catch (InterruptedException e) {
                LogHelper.getLogger().warning(e);
            }

        }
        else {
            LogHelper.getLogger().debug("Peer " + connHandler.getRemotePeerId() + " is trying to connect but a connection already exists");
        }
        return true;
    }

    public synchronized void chokedPeers(Collection<Integer> chokedPeersIds) {
        for (ConnectionManager ch : connHandlers) {
            if (chokedPeersIds.contains(ch.getRemotePeerId())) {
                LogHelper.getLogger().debug("Choking " + ch.getRemotePeerId());
                ch.send(new Choke());
            }
        }
    }

    public synchronized void unchokedPeers(Collection<Integer> unchokedPeersIds) {
        for (ConnectionManager ch : connHandlers) {
            if (unchokedPeersIds.contains(ch.getRemotePeerId())) {
                LogHelper.getLogger().debug("Unchoking " + ch.getRemotePeerId());
                ch.send(new Unchoke());
            }
        }
    }
}
