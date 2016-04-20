package networks.impl;

import networks.impl.File.FileUtility;
import networks.models.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Created by mallem on 4/20/16.
 */
public class MessageManager {

    private boolean chokedByRemotePeer;
    private final int remotePeerId;
    private final FileUtility fileUtil;
    private final PeerManager peerMgr;
    //private final EventLogger _eventLogger;


    /* TODO: Fix the eventlogger in this chatha!
    MessageManager(int remotePeerId, FileUtility fileMgr, PeerManager peerMgr, EventLogger eventLogger) {
        chokedByRemotePeer = true;
        fileUtil = fileMgr;
        this.peerMgr = peerMgr;
        this.remotePeerId = remotePeerId;
        //_eventLogger = eventLogger;
    }*/

    MessageManager(int remotePeerId, FileUtility fileMgr, PeerManager peerMgr) {
        chokedByRemotePeer = true;
        fileUtil = fileMgr;
        this.peerMgr = peerMgr;
        this.remotePeerId = remotePeerId;
    }

    public Message handle(HandShake handshake) {
        BitSet bitset = fileUtil.getReceivedParts();
        if (!bitset.isEmpty()) {
            return (new Bitfield(bitset));
        }
        return null;
    }

    public Message handle(Message msg) {
        switch (msg.getMessageType()) {
            case Choke: {
                chokedByRemotePeer = true;
                //_eventLogger.chokeMessage(remotePeerId);
                return null;
            }
            case Unchoke: {
                chokedByRemotePeer = false;
                //_eventLogger.unchokeMessage(remotePeerId);
                return requestPiece();
            }
            case Interested: {
                //_eventLogger.interestedMessage(remotePeerId);
                peerMgr.addInterestPeer(remotePeerId);
                return null;
            }
            case NotInterested: {
                //_eventLogger.notInterestedMessage(remotePeerId);
                peerMgr.removeInterestPeer(remotePeerId);
                return null;
            }
            case Have: {
                Have have = (Have) msg;
                final int pieceId = have.getPartIndex();
                //_eventLogger.haveMessage(remotePeerId, pieceId);
                peerMgr.haveArrived(remotePeerId, pieceId);

                if (fileUtil.getReceivedParts().get(pieceId)) {
                    return new NotInterested();
                } else {
                    return new Interested();
                }
            }
            case BitField: {
                Bitfield bitfield = (Bitfield) msg;
                BitSet bitset = bitfield.getBitSet();
                peerMgr.bitfieldArrived(remotePeerId, bitset);

                bitset.andNot(fileUtil.getReceivedParts());
                if (bitset.isEmpty()) {
                    return new NotInterested();
                } else {
                    // the peer has parts that this peer does not have
                    return new Interested();
                }
            }
            case Request: {
                Request request = (Request) msg;
                if (peerMgr.canUploadToPeer(remotePeerId)) {
                    byte[] piece = fileUtil.getPiece(ByteBuffer.wrap(Arrays.copyOfRange(msg.getPayload(), 0, 4))
                            .order(ByteOrder.BIG_ENDIAN).getInt());
                    if (piece != null) {
                        return new Piece(ByteBuffer.wrap(Arrays.copyOfRange(msg.getPayload(), 0, 4))
                                .order(ByteOrder.BIG_ENDIAN).getInt(), piece);
                    }
                }
                return null;
            }
            case Piece: {
                Piece piece = (Piece) msg;
                fileUtil.addPart(ByteBuffer.wrap(Arrays.copyOfRange(msg.getPayload(), 0, 4))
                        .order(ByteOrder.BIG_ENDIAN).getInt(), piece.getContent());
                peerMgr.receivedPart(remotePeerId, piece.getContent().length);
                //_eventLogger.pieceDownloadedMessage(remotePeerId, piece.getPieceIndex(), fileUtil.getNumberOfReceivedParts());
                return requestPiece();
            }
        }

        return null;
    }

    private Message requestPiece() {
        if (!chokedByRemotePeer) {
            int partId = fileUtil.getPartToRequest(peerMgr.getReceivedParts(remotePeerId));
            if (partId >= 0) {
               // LogHelper.getLogger().debug("Requesting part " + partId + " to " + remotePeerId);
                return new Request(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(partId).array());
            }
            else {
               // LogHelper.getLogger().debug("No parts can be requested to " + remotePeerId);
            }
        }
        return null;
    }

}
