package impl;

import impl.File.FileUtility;
import models.HandShake;
import models.Message;
import models.Request;
import utilities.PayloadReader;
import utilities.PayloadWriter;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mallem on 4/19/16.
 */
public class ConnectionManager implements Runnable {

    private Socket socket;
    private final int localPeerId;
    private final FileUtility fileUtility;
    private final PeerManager peerMgr;
    private final boolean isConnectingPeer;
    private final int expectedRemotePeerId;
    private final PayloadWriter payloadWriter;
    private final AtomicInteger remotePeerId;
    private final BlockingQueue<Message> _queue = new LinkedBlockingQueue();

    public ConnectionManager(int localPeerId, Socket socket, FileUtility fileMgr, PeerManager peerMgr)
            throws IOException {
        this(localPeerId, false, -1, socket, fileMgr, peerMgr);
    }

    public ConnectionManager(int localPeerId, boolean isConnectingPeer, int expectedRemotePeerId,
                             Socket socket, FileUtility fileUtil, PeerManager peerMgr) throws IOException {
        this.socket = socket;
        this.localPeerId = localPeerId;
        this.isConnectingPeer = isConnectingPeer;
        this.expectedRemotePeerId = expectedRemotePeerId;
        fileUtility = fileUtil;
        this.peerMgr = peerMgr;
        payloadWriter = new PayloadWriter(this.socket.getOutputStream());
        remotePeerId = new AtomicInteger(-1);
    }

    public int getRemotePeerId() {
        return remotePeerId.get();
    }

    @Override
    public void run() {
        new Thread() {

            private boolean remotePeerIsChoked = true;

            @Override
            public void run() {
                Thread.currentThread().setName(getClass().getName() + "-" + remotePeerId + "-sending thread");
                while (true) {
                    try {
                        final Message message = _queue.take();
                        if (message == null) {
                            continue;
                        }
                        if (remotePeerId.get() != -1) {
                            switch (message.getMessageType()) {
                                case Choke: {
                                    if (!remotePeerIsChoked) {
                                        remotePeerIsChoked = true;
                                        sendInternal(message);
                                    }
                                    break;
                                }

                                case Unchoke: {
                                    if (remotePeerIsChoked) {
                                        remotePeerIsChoked = false;
                                        sendInternal(message);
                                    }
                                    break;
                                }

                                default:
                                    sendInternal(message);
                            }
                        } else {
                           // LogHelper.getLogger().debug("cannot send message of type "
                                    //+ message.getType() + " because the remote peer has not handshaked yet.");
                        }
                    } catch (IOException ex) {
                       // LogHelper.getLogger().warning(ex);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }.start();

        try {
            final PayloadReader in = new PayloadReader(socket.getInputStream());

            // Send handshake
            payloadWriter.writeObject(new HandShake(ByteBuffer.allocate(4)
                    .order(ByteOrder.BIG_ENDIAN).putInt(localPeerId).array()));

            // Receive and check handshake
            HandShake rcvdHandshake = (HandShake) in.readObject();
            /* TODO: I think this shud work */
            remotePeerId.set(localPeerId);
            Thread.currentThread().setName(getClass().getName() + "-" + remotePeerId.get());
            final EventLogger eventLogger = new EventLogger(localPeerId);
            final MessageHandler msgHandler = new MessageHandler(remotePeerId.get(), fileUtility, peerMgr, eventLogger);
            if (isConnectingPeer && (remotePeerId.get() != expectedRemotePeerId)) {
                throw new Exception("Remote peer id " + remotePeerId + " does not match with the expected id: " + expectedRemotePeerId);
            }

            // Handshake successful
            eventLogger.peerConnection(remotePeerId.get(), isConnectingPeer);

            sendInternal(msgHandler.handle(rcvdHandshake));
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
        LogHelper.getLogger().warning(Thread.currentThread().getName()
                + " terminating, messages will no longer be accepted.");
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
        _queue.add(message);
    }

    private synchronized void sendInternal(Message message) throws IOException {
        if (message != null) {
            payloadWriter.writeObject(message);
            switch (message.getMessageType()) {
                case Request: {
                    new java.util.Timer().schedule(
                            new RequestTimer((Request) message, fileUtility, payloadWriter, message, remotePeerId.get()),
                            peerMgr.getUnchokingInterval() * 2
                    );
                }
            }
        }
    }
}
