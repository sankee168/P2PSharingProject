package networks.impl;

import networks.impl.File.FileOperations;
import networks.utilities.LogHelper;

import java.io.*;
import java.io.FilenameFilter;

public class Destination {

    private final File file;
    private final File chunksDir;
    private static final String chunksLocation = "files/parts/";
    FileOperations fileOperations;

    public Destination(int peerId, String fileName) {
        chunksDir = new File("./peer_" + peerId + "/" + chunksLocation + fileName);
        chunksDir.mkdirs();
        file = new File(chunksDir.getParent() + "/../" + fileName);
        fileOperations = new FileOperations(file, chunksDir);
    }

    public byte[][] getAllChunksAsByteArrays() {
        File[] files = chunksDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return true;
            }
        });
        byte[][] ba = new byte[files.length][getChunkAsByteArray(1).length];
        for (File file : files) {
            ba[Integer.parseInt(file.getName())] = getByteArrayFromFile(file);
        }
        return ba;
    }

    public byte[] getChunkAsByteArray(int partId) {
        File file = new File(chunksDir.getAbsolutePath() + "/" + partId);
        return getByteArrayFromFile(file);
    }

    public void writeByteArrayAsFileChunk(byte[] part, int partId) {
        FileOutputStream fos;
        File ofile = new File(chunksDir.getAbsolutePath() + "/" + partId);
        try {
            fos = new FileOutputStream(ofile);
            fos.write(part);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            LogHelper.getLogger().warning(e);
        } catch (IOException e) {
            LogHelper.getLogger().warning(e);
        }
    }

    private byte[] getByteArrayFromFile(File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] fileBytes = new byte[(int) file.length()];
            int bytesRead = fis.read(fileBytes, 0, (int) file.length());
            fis.close();
            assert (bytesRead == fileBytes.length);
            assert (bytesRead == (int) file.length());
            return fileBytes;
        } catch (FileNotFoundException e) {
            LogHelper.getLogger().warning(e);
        } catch (IOException e) {
            LogHelper.getLogger().warning(e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                }
            }
        }
        return null;

    }

    public void splitFile(int partSize) {
        fileOperations.splitFile(partSize);
        LogHelper.getLogger().debug("File has been split");
    }

    public void mergeFile(int partSize) {
        fileOperations.mergeFile(partSize);
        LogHelper.getLogger().debug("File has been merged");
    }
}
