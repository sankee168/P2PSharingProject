package networks.impl;

import networks.impl.File.FileUtility;
import networks.models.HandShake;
import networks.models.Message;
import networks.models.Request;
import networks.utilities.EventLogger;
import networks.utilities.LogHelper;
import networks.utilities.PayloadReader;
import networks.utilities.PayloadWriter;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionManager implements Runnable {

    private int localPeerId;
    private Socket socket;
    private PayloadWriter payloadWriter;
    private FileUtility fileUtil;
    private PeerManager peerManager;
    private boolean isConnecting;
    private int expectedPeerId;
    private AtomicInteger remotePeerId;
    private BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();

    public ConnectionManager(int localPeerId, Socket socket, FileUtility fileMgr, PeerManager peerMgr)
            throws IOException {
        this(localPeerId, false, -1, socket, fileMgr, peerMgr);
    }

    public ConnectionManager(int localPeerId, boolean isConnectingPeer, int expectedRemotePeerId,
                             Socket socket, FileUtility fileMgr, PeerManager peerMgr) throws IOException {
        this.socket = socket;
        this.localPeerId = localPeerId;
        this.isConnecting = isConnectingPeer;
        this.expectedPeerId = expectedRemotePeerId;
        this.fileUtil = fileMgr;
        this.peerManager = peerMgr;
        this.payloadWriter = new PayloadWriter(this.socket.getOutputStream());
        this.remotePeerId = new AtomicInteger(-1);
    }

    public int getRemotePeerId() {
        return remotePeerId.get();
    }

    public void run() {
        new Thread() {

            private boolean isRemoteChoked = true;

            @Override
            public void run() {
                Thread.currentThread().setName(getClass().getName() + "-" + remotePeerId + "-sending thread");
                while (true) {
                    try {
                        final Message message = queue.take();
                        if (message == null) {
                            continue;
                        }
                        if (remotePeerId.get() != -1) {
                            switch (message.getMessageType()) {
                                case Choke: {
                                    if (!isRemoteChoked) {
                                        isRemoteChoked = true;
                                        sendInternal(message);
                                    }
                                    break;
                                }

                                case Unchoke: {
                                    if (isRemoteChoked) {
                                        isRemoteChoked = false;
                                        sendInternal(message);
                                    }
                                    break;
                                }

                                default:
                                    sendInternal(message);
                            }
                        } else {
                            LogHelper.getLogger().debug("cannot send message of type "
                                    + message.getMessageType() + " because the remote peer has not handshaked yet.");
                        }
                    } catch (IOException ex) {
                        LogHelper.getLogger().warning(ex);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }.start();

        try {
            final PayloadReader in = new PayloadReader(socket.getInputStream());

            // Send handshake
            payloadWriter.writeObject(new HandShake(localPeerId));

            // Receive and check handshake
            HandShake rcvdHandshake = (HandShake) in.readObject();
            remotePeerId.set(ByteBuffer.wrap(rcvdHandshake.getPeerIdBits()).order(ByteOrder.BIG_ENDIAN).getInt());
            Thread.currentThread().setName(getClass().getName() + "-" + remotePeerId.get());
            final EventLogger eventLogger = new EventLogger(localPeerId);
            final MessageManager msgHandler = new MessageManager(remotePeerId.get(), fileUtil, peerManager, eventLogger);
            if (isConnecting && (remotePeerId.get() != expectedPeerId)) {
                throw new Exception("Remote peer id " + remotePeerId + " does not match with the expected id: " + expectedPeerId);
            }

            // Handshake successful
            eventLogger.peerConnection(remotePeerId.get(), isConnecting);

            sendInternal(msgHandler.handle());
            while (true) {
                try {
                    sendInternal(msgHandler.handle((Message) in.readObject()));
                } catch (Exception ex) {
                    LogHelper.getLogger().warning(ex);
                    break;
                }
            }
        } catch (Exception ex) {
            LogHelper.getLogger().warning(ex);
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConnectionManager) {
            return ((ConnectionManager) obj).remotePeerId == remotePeerId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + localPeerId;
        return hash;
    }

    public void send(final Message message) {
        queue.add(message);
    }

    private synchronized void sendInternal(Message message) throws IOException {
        if (message != null) {
            payloadWriter.writeObject(message);
            switch (message.getMessageType()) {
                case Request: {
                    new java.util.Timer().schedule(
                            new RequestTimer((Request) message, fileUtil, payloadWriter, message, remotePeerId.get()),
                            peerManager.getUnchokingInterval() * 2
                    );
                }
            }
        }
    }

}
