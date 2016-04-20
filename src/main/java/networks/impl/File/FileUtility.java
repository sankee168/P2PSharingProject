package networks.impl.File;

import networks.impl.Destination;
import networks.utilities.LogHelper;
import networks.utilities.PropertyFileUtility;

import java.util.BitSet;
import java.util.LinkedList;

/**
 * Created by mallem on 4/20/16.
 */
public class FileUtility {


    private BitSet receivedChunks;
    private final LinkedList<FileEvent> fileEvents = new LinkedList();
    private Destination destination;
    private final double chunkSize;
    private final int bitsetSize;
    private final BitSet requestedChunks;
    private final long timeOut;

    public FileUtility (int peerId, PropertyFileUtility conf) {
        this (peerId,
                conf.getStringValue("FileName"),
                conf.getIntegerValue("FileSize"),
                conf.getIntegerValue("PieceSize"),
                conf.getLongValue("UnchokingInterval"));
    }

    /**
     *
     * @param peerId the id of this peer
     * @param fileName the file being downloaded
     * @param fileSize the size of the file being downloaded
     * @param partSize the maximum size of a part
     */
    FileUtility(int peerId, String fileName, int fileSize, int partSize, long unchokingInterval) {
        chunkSize = partSize;
        bitsetSize = (int) Math.ceil (fileSize/ chunkSize);
        LogHelper.getLogger().debug ("File size set to " + fileSize +  "\tPart size set to " + chunkSize + "\tBitset size set to " + bitsetSize);
        receivedChunks = new BitSet (bitsetSize);
        destination = new Destination(peerId, fileName);
        requestedChunks = new BitSet(bitsetSize);
        timeOut = unchokingInterval * 2;
    }

    /**
     *
     * @param partIdx
     * @param part
     */
    public synchronized void addPart (int partIdx, byte[] part) {

        // TODO: write part on file, at the specified directroy
        final boolean isNewPiece = !receivedChunks.get(partIdx);
        receivedChunks.set (partIdx);

        if (isNewPiece) {
            destination.writeByteArrayAsFilePart(part, partIdx);
            for (FileEvent listener : fileEvents) {
                listener.pieceArrived (partIdx);
            }
        }
        if (isFileCompleted()) {
            destination.mergeFile(receivedChunks.cardinality());
            for (FileEvent listener : fileEvents) {
                listener.fileCompleted();
            }
        }
    }

    /**
     * @param availableParts parts that are available at the remote peer
     * @return the ID of the part to request, if any, or a negative number in
     * case all the missing parts are already being requested or the file is
     * complete.
     */
    public synchronized int getPartToRequest(BitSet availableParts) {
        availableParts.andNot(getReceivedParts());
        return getNextChunk(availableParts);
    }

    public synchronized BitSet getReceivedParts () {
        return (BitSet) receivedChunks.clone();
    }

    synchronized public boolean hasPart(int pieceIndex) {
        return receivedChunks.get(pieceIndex);
    }

    /**
     * Set all parts as received.
     */
    public synchronized void setAllParts()
    {
        for (int i = 0; i < bitsetSize; i++) {
            receivedChunks.set(i, true);
        }
        LogHelper.getLogger().debug("Received parts set to: " + receivedChunks.toString());
    }

    public synchronized int getNumberOfReceivedParts() {
        return receivedChunks.cardinality();
    }

    public byte[] getPiece (int partId) {
        byte[] piece = destination.getPartAsByteArray(partId);
        return piece;
    }

    public void registerListener (FileEvent listener) {
        fileEvents.add (listener);
    }

    public void splitFile(){
        destination.splitFile((int) chunkSize);
    }

    public byte[][] getAllPieces(){
        return destination.getAllPartsAsByteArrays();
    }

    public int getBitmapSize() {
        return bitsetSize;
    }

    private boolean isFileCompleted() {
        for (int i = 0; i < bitsetSize; i++) {
            if (!receivedChunks.get(i)) {
                return false;
            }
        }
        return true;
    }

    synchronized int getNextChunk(BitSet requestabableParts) {
        requestabableParts.andNot(requestedChunks);
        if (!requestabableParts.isEmpty()) {
            final int partId = pickRandomSetIndexFromBitSet(requestabableParts);
            requestedChunks.set(partId);

            // Make the part requestable again in _timeoutInMillis
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            synchronized (requestedChunks) {
                                requestedChunks.clear(partId);
                                LogHelper.getLogger().debug("clearing requested parts for pert " + partId);
                            }
                        }
                    },
                    timeOut
            );
            return partId;
        }
        return -1;
    }

    public int pickRandomSetIndexFromBitSet (BitSet bitset) {
        if (bitset.isEmpty()) {
            throw new RuntimeException ("The bitset is empty, cannot find a set element");
        }
        // Generate list of set elements in the format that follows: { 2, 4, 5, ...}
        String set = bitset.toString();
        // Separate the elements, and pick one randomly
        String[] indexes = set.substring(1, set.length()-1).split(",");
        return Integer.parseInt(indexes[(int)(Math.random()*(indexes.length-1))].trim());
    }
}
