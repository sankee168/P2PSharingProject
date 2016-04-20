package networks.impl;

import networks.impl.File.FileUtility;
import networks.models.*;
import networks.utilities.EventLogger;
import networks.utilities.LogHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;

public class MessageManager {

    private boolean isChoked;
    private final int remotePeerId;
    private final FileUtility fileUtil;
    private final PeerManager peerMgr;
    private final EventLogger eventLogger;

    MessageManager(int remotePeerId, FileUtility fileUtil, PeerManager peerMgr, EventLogger eventLogger) {
        isChoked = true;
        this.fileUtil = fileUtil;
        this.peerMgr = peerMgr;
        this.remotePeerId = remotePeerId;
        this.eventLogger = eventLogger;
    }

    public Message handle() {
        BitSet bitset = fileUtil.getReceivedChunks();
        if (!bitset.isEmpty()) {
            return (new Bitfield(bitset));
        }
        return null;
    }

    public Message handle(Message msg) {
        switch (msg.getMessageType()) {
            case Choke: {
                isChoked = true;
                eventLogger.chokeMessage(remotePeerId);
                return null;
            }
            case Unchoke: {
                isChoked = false;
                eventLogger.unchokeMessage(remotePeerId);
                return requestChunk();
            }
            case Interested: {
                eventLogger.interestedMessage(remotePeerId);
                peerMgr.addInterestPeer(remotePeerId);
                return null;
            }
            case NotInterested: {
                eventLogger.notInterestedMessage(remotePeerId);
                peerMgr.removeInterestPeer(remotePeerId);
                return null;
            }
            case Have: {
                Have have = (Have) msg;
                final int pieceId = have.getPartIndex();
                eventLogger.haveMessage(remotePeerId, pieceId);
                peerMgr.haveArrived(remotePeerId, pieceId);

                if (fileUtil.getReceivedChunks().get(pieceId)) {
                    return new NotInterested();
                } else {
                    return new Interested();
                }
            }
            case BitField: {
                Bitfield bitfield = (Bitfield) msg;
                BitSet bitset = bitfield.getBitSet();
                peerMgr.bitfieldArrived(remotePeerId, bitset);

                bitset.andNot(fileUtil.getReceivedChunks());
                if (bitset.isEmpty()) {
                    return new NotInterested();
                } else {
                    return new Interested();
                }
            }
            case Request: {
                if (peerMgr.canUploadToPeer(remotePeerId)) {
                    byte[] piece = fileUtil.getChunk(ByteBuffer.wrap(Arrays.copyOfRange(msg.getPayload(), 0, 4))
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
                fileUtil.addChunk(ByteBuffer.wrap(Arrays.copyOfRange(msg.getPayload(), 0, 4))
                        .order(ByteOrder.BIG_ENDIAN).getInt(), piece.getContent());
                peerMgr.receivedPart(remotePeerId, piece.getContent().length);
                eventLogger.pieceDownloadedMessage(remotePeerId, ByteBuffer.wrap(Arrays.copyOfRange(msg.getPayload(), 0, 4))
                        .order(ByteOrder.BIG_ENDIAN).getInt(), fileUtil.getNumberOfReceivedChunks());
                return requestChunk();
            }
        }
        return null;
    }

    private Message requestChunk() {
        if (!isChoked) {
            int chunkId = fileUtil.getChunkToRequest(peerMgr.getReceivedParts(remotePeerId));
            if (chunkId >= 0) {
                LogHelper.getLogger().debug("Requesting chunk " + chunkId + " to " + remotePeerId);
                return new Request(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(chunkId).array());
            }
            else {
                LogHelper.getLogger().debug("No chunks can be requested to " + remotePeerId);
            }
        }
        return null;
    }

}
