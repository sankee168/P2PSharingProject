package networks.impl.File;

import networks.impl.Destination;
import networks.utilities.LogHelper;
import networks.utilities.PropertyFileUtility;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

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


    FileUtility(int peerId, String fileName, int fileSize, int partSize, long unchokingInterval) {
        chunkSize = partSize;
        bitsetSize = (int) Math.ceil (fileSize/ chunkSize);
        LogHelper.getLogger().debug ("File size set to " + fileSize +  "\tPart size set to " + chunkSize + "\tBitset size set to " + bitsetSize);
        receivedChunks = new BitSet (bitsetSize);
        destination = new Destination(peerId, fileName);
        requestedChunks = new BitSet(bitsetSize);
        timeOut = unchokingInterval * 2;
    }


    public synchronized void addChunk(int partIdx, byte[] part) {

        final boolean isNewPiece = !receivedChunks.get(partIdx);
        receivedChunks.set (partIdx);

        if (isNewPiece) {
            destination.writeByteArrayAsFileChunk(part, partIdx);
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


    public synchronized int getChunkToRequest(BitSet availableParts) {
        availableParts.andNot(getReceivedChunks());
        return getNextChunk(availableParts);
    }

    public synchronized BitSet getReceivedChunks() {
        return (BitSet) receivedChunks.clone();
    }

    synchronized public boolean hasChunk(int pieceIndex) {
        return receivedChunks.get(pieceIndex);
    }

    public synchronized void setAllChunks()
    {
        for (int i = 0; i < bitsetSize; i++) {
            receivedChunks.set(i, true);
        }
        LogHelper.getLogger().debug("Received chunks set to: " + receivedChunks.toString());
    }

    public synchronized int getNumberOfReceivedChunks() {
        return receivedChunks.cardinality();
    }

    public byte[] getChunk(int chunkId) {
        byte[] chunk = destination.getChunkAsByteArray(chunkId);
        return chunk;
    }

    public void registerEvent(FileEvent event) {
        fileEvents.add (event);
    }

    public void splitFile(){
        destination.splitFile((int) chunkSize);
    }

    public byte[][] getAllPieces(){
        return destination.getAllChunksAsByteArrays();
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

    synchronized int getNextChunk(BitSet requestChunk) {
        requestChunk.andNot(requestedChunks);
        if (!requestChunk.isEmpty()) {
            final int partId = pickRandomSetIndexFromBitSet(requestChunk);
            requestedChunks.set(partId);

            new Timer().schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            synchronized (requestedChunks) {
                                requestedChunks.clear(partId);
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
        String set = bitset.toString();
        String[] indexes = set.substring(1, set.length()-1).split(",");
        return Integer.parseInt(indexes[(int)(Math.random()*(indexes.length-1))].trim());
    }
}
