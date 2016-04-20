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

/**
 * Created by sank on 4/20/16.
 */
public class Process implements Runnable, FileEvent, PeerEvents{
    private final int _peerId;
    private final int _port;
    private final boolean _hasFile;
    private final PropertyFileUtility _conf;
    private final FileUtility _fileMgr;
    private final PeerManager _peerMgr;
    private final EventLogger _eventLogger;
    private final AtomicBoolean _fileCompleted = new AtomicBoolean(false);
    private final AtomicBoolean _peersFileCompleted = new AtomicBoolean(false);
    private final AtomicBoolean _terminate = new AtomicBoolean(false);
    private final Collection<ConnectionManager> _connHandlers =
            Collections.newSetFromMap(new ConcurrentHashMap<ConnectionManager, Boolean>());

    public Process(int peerId, String address, int port, boolean hasFile, Collection<RemotePeerInfo> peerInfo, PropertyFileUtility conf) {
        _peerId = peerId;
        _port = port;
        _hasFile = hasFile;
        _conf = conf;
        _fileMgr = new FileUtility(_peerId, _conf);
        ArrayList<RemotePeerInfo> remotePeers = new ArrayList<RemotePeerInfo>(peerInfo);
        for (RemotePeerInfo ri : remotePeers) {
            if (ri.getPeerId() == peerId) {
                remotePeers.remove(ri);
                break;
            }
        }
        _peerMgr = new PeerManager(_peerId, remotePeers, _fileMgr.getBitmapSize(), _conf);
        _eventLogger = new EventLogger(peerId);
        _fileCompleted.set(_hasFile);
    }

    public void init() {
        _fileMgr.registerEvent(this);
        _peerMgr.registerListener(this);

        if (_hasFile) {
//            System.out.println("Splitting File");
            LogHelper.getLogger().debug("Spltting file");
            _fileMgr.splitFile();
            _fileMgr.setAllChunks();
        }
        else {
//            System.out.println("Peer does not have file");
            LogHelper.getLogger().debug("Peer does not have file " + _peerId);
        }

        // Start PeerMnager Thread
        Thread t = new Thread(_peerMgr);
        t.setName(_peerMgr.getClass().getName());
        t.start();
    }

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(_port);
            while (!_terminate.get()) {
                try {
                    LogHelper.getLogger().debug(Thread.currentThread().getName() + ": Peer " + _peerId + " listening on port " + _port + ".");
                    addConnHandler(new ConnectionManager(_peerId, serverSocket.accept(), _fileMgr, _peerMgr));

                } catch (Exception e) {
//                    System.out.println("asdgagshfdghasfdha");
                    LogHelper.getLogger().warning(e);
                }
            }
        } catch (IOException ex) {
//            System.out.println("a,sdhjgashjgdjkasgdjhas");
            LogHelper.getLogger().warning(ex);
        } finally {
//            System.out.println("amsgdvasjh,gdjahsgdkjahsg");
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
                    if (addConnHandler(new ConnectionManager(_peerId, true, peer.getPeerId(),
                            socket, _fileMgr, _peerMgr))) {
                        iter.remove();
//                        System.out.println("asdhgaksjgdjasgd");
                        LogHelper.getLogger().debug(" Connected to peer: " + peer.getPeerId()
                                + " (" + peer.getPeerAddress() + ":" + peer.getPeerPort() + ")");

                    }
                }
                catch (ConnectException ex) {
//                    System.out.println("kjhlkhfkjvhkbvhnlukgetv8lo,mk");
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
//                    System.out.println("ashdgajsdgjas");
                    LogHelper.getLogger().warning(ex);
                }
            }
            while (iter.hasNext());

            // Keep trying until they all connect
            iter = peersToConnectTo.iterator();
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
            }
        }
    }

    public void neighborsCompletedDownload() {
        LogHelper.getLogger().debug("all peers completed download");
//        System.out.println("adjhgaskjdghaskjdgaskhjg");
        _peersFileCompleted.set(true);
        if (_fileCompleted.get() && _peersFileCompleted.get()) {
            // The process can quit
            _terminate.set(true);
            System.exit(0);
        }
    }

    public synchronized void fileCompleted() {
//        System.out.println("alksdghaksjhdgakjhsgdhsaghj");
        LogHelper.getLogger().debug("local peer completed download");
        _eventLogger.fileDownloadedMessage();
        _fileCompleted.set(true);
        if (_fileCompleted.get() && _peersFileCompleted.get()) {
            // The process can quit
            _terminate.set(true);
            System.exit(0);
        }
    }

    public synchronized void pieceArrived(int partIdx) {
        for (ConnectionManager connHanlder : _connHandlers) {
            connHanlder.send(new Have(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(partIdx).array()));
            if (!_peerMgr.isInteresting(connHanlder.getRemotePeerId(), _fileMgr.getReceivedChunks())) {
                connHanlder.send(new NotInterested());
            }
        }
    }

    private synchronized boolean addConnHandler(ConnectionManager connHandler) {
        if (!_connHandlers.contains(connHandler)) {
            _connHandlers.add(connHandler);
            new Thread(connHandler).start();
            try {
                wait(10);
            } catch (InterruptedException e) {
//                System.out.println("jahgsdhkjagsdkjasgh");
                LogHelper.getLogger().warning(e);
            }

        }
        else {
//            System.out.println("akjhsgdhakjsgdhkjasgfdhkjsagfkhdj");
            LogHelper.getLogger().debug("Peer " + connHandler.getRemotePeerId() + " is trying to connect but a connection already exists");
        }
        return true;
    }

    public synchronized void chokedPeers(Collection<Integer> chokedPeersIds) {
        for (ConnectionManager ch : _connHandlers) {
            if (chokedPeersIds.contains(ch.getRemotePeerId())) {
//                System.out.println("akjhsdjasgdhjasgd");
                LogHelper.getLogger().debug("Choking " + ch.getRemotePeerId());
                ch.send(new Choke());
            }
        }
    }

    public synchronized void unchokedPeers(Collection<Integer> unchokedPeersIds) {
        for (ConnectionManager ch : _connHandlers) {
            if (unchokedPeersIds.contains(ch.getRemotePeerId())) {
//                System.out.println("akjshgdahjsgfdhjafsjhgdfash");
                LogHelper.getLogger().debug("Unchoking " + ch.getRemotePeerId());
                ch.send(new Unchoke());
            }
        }
    }
}
