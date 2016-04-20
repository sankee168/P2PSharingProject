package networks.impl;

import networks.impl.File.FileUtility;
import networks.models.HandShake;
import networks.models.Message;
import networks.models.Request;
import networks.utilities.EventLogger;
import networks.utilities.LogHelper;
import networks.utilities.PayloadReader;
import networks.utilities.PayloadWriter;

import java.io.File;
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

    private static final int PEER_ID_UNSET = -1;

    private final int _localPeerId;
    private final Socket _socket;
    private final PayloadWriter _out;
    private final FileUtility _fileMgr;
    private final PeerManager _peerMgr;
    private final boolean _isConnectingPeer;
    private final int _expectedRemotePeerId;
    private final AtomicInteger _remotePeerId;
    private final BlockingQueue<Message> _queue = new LinkedBlockingQueue<Message>();

    public ConnectionManager(int localPeerId, Socket socket, FileUtility fileMgr, PeerManager peerMgr)
            throws IOException {
        this(localPeerId, false, -1, socket, fileMgr, peerMgr);
    }

    public ConnectionManager(int localPeerId, boolean isConnectingPeer, int expectedRemotePeerId,
                             Socket socket, FileUtility fileMgr, PeerManager peerMgr) throws IOException {
        _socket = socket;
        _localPeerId = localPeerId;
        _isConnectingPeer = isConnectingPeer;
        _expectedRemotePeerId = expectedRemotePeerId;
        _fileMgr = fileMgr;
        _peerMgr = peerMgr;
        _out = new PayloadWriter(_socket.getOutputStream());
        _remotePeerId = new AtomicInteger(PEER_ID_UNSET);
    }

    public int getRemotePeerId() {
        return _remotePeerId.get();
    }

    public void run() {
        new Thread() {

            private boolean _remotePeerIsChoked = true;

            @Override
            public void run() {
                Thread.currentThread().setName(getClass().getName() + "-" + _remotePeerId + "-sending thread");
                while (true) {
                    try {
                        final Message message = _queue.take();
                        if (message == null) {
                            continue;
                        }
                        if (_remotePeerId.get() != PEER_ID_UNSET) {
                            switch (message.getMessageType()) {
                                case Choke: {
                                    if (!_remotePeerIsChoked) {
                                        _remotePeerIsChoked = true;
                                        sendInternal(message);
                                    }
                                    break;
                                }

                                case Unchoke: {
                                    if (_remotePeerIsChoked) {
                                        _remotePeerIsChoked = false;
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
            final PayloadReader in = new PayloadReader(_socket.getInputStream());

            // Send handshake
            _out.writeObject(new HandShake(_localPeerId));

            // Receive and check handshake
            HandShake rcvdHandshake = (HandShake) in.readObject();
            _remotePeerId.set(ByteBuffer.wrap(rcvdHandshake.getPeerIdBits()).order(ByteOrder.BIG_ENDIAN).getInt());
            Thread.currentThread().setName(getClass().getName() + "-" + _remotePeerId.get());
            final EventLogger eventLogger = new EventLogger(_localPeerId);
            final MessageManager msgHandler = new MessageManager(_remotePeerId.get(), _fileMgr, _peerMgr, eventLogger);
            if (_isConnectingPeer && (_remotePeerId.get() != _expectedRemotePeerId)) {
                throw new Exception("Remote peer id " + _remotePeerId + " does not match with the expected id: " + _expectedRemotePeerId);
            }

            // Handshake successful
            eventLogger.peerConnection(_remotePeerId.get(), _isConnectingPeer);

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
                _socket.close();
            } catch (Exception e) {
            }
        }
        LogHelper.getLogger().warning(Thread.currentThread().getName()
                + " terminating, messages will no longer be accepted.");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConnectionManager) {
            return ((ConnectionManager) obj)._remotePeerId == _remotePeerId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + _localPeerId;
        return hash;
    }

    public void send(final Message message) {
        _queue.add(message);
    }

    private synchronized void sendInternal(Message message) throws IOException {
        if (message != null) {
            _out.writeObject(message);
            switch (message.getMessageType()) {
                case Request: {
                    new java.util.Timer().schedule(
                            new RequestTimer((Request) message, _fileMgr, _out, message, _remotePeerId.get()),
                            _peerMgr.getUnchokingInterval() * 2
                    );
                }
            }
        }
    }

}
