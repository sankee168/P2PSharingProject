package impl;

import impl.File.FileOperations;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.io.FilenameFilter;

/**
 * Created by sank on 4/20/16.
 */
//todo: change everything in here including name of file
public class Destination {

    private final File _file;
    private final File _partsDir;
    private static final String partsLocation = "files/parts/";
    FileOperations fileOperations = new FileOperations();

    public Destination(int peerId, String fileName) {
        _partsDir = new File("./peer_" + peerId + "/" + partsLocation + fileName);
        _partsDir.mkdirs();
        _file = new File(_partsDir.getParent() + "/../" + fileName);
    }

    public byte[][] getAllPartsAsByteArrays() {
//        File[] files = _partsDir.listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String name) {
//                return true;
//            }
//        });
        File[] files = _partsDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return true;
            }
        });
        byte[][] ba = new byte[files.length][getPartAsByteArray(1).length];
        for (File file : files) {
            ba[Integer.parseInt(file.getName())] = getByteArrayFromFile(file);
        }
        return ba;
    }

    public byte[] getPartAsByteArray(int partId) {
        File file = new File(_partsDir.getAbsolutePath() + "/" + partId);
        return getByteArrayFromFile(file);
    }

    public void writeByteArrayAsFilePart(byte[] part, int partId) {
        FileOutputStream fos;
        File ofile = new File(_partsDir.getAbsolutePath() + "/" + partId);
        try {
            fos = new FileOutputStream(ofile);
            fos.write(part);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            //todo: log here
//            LogHelper.getLogger().warning(e);
        } catch (IOException e) {
            //todo: log here
//            LogHelper.getLogger().warning(e);
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
            //todo: log here
//            LogHelper.getLogger().warning(e);
        } catch (IOException e) {
            //todo: log here
//            LogHelper.getLogger().warning(e);
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

        fileOperations.splitFile(_file, partSize);
        //todo: log here
//        LogHelper.getLogger().debug("File has been split");
    }

    public void mergeFile(int numParts) {
        File ofile = _file;
        FileOutputStream fos;
        FileInputStream fis;
        byte[] fileBytes;
        int bytesRead = 0;
        List<File> list = new ArrayList<File>();
        for (int i = 0; i < numParts; i++) {
            list.add(new File(_partsDir.getPath() + "/" + i));
        }
        try {
            fos = new FileOutputStream(ofile);
            for (File file : list) {
                fis = new FileInputStream(file);
                fileBytes = new byte[(int) file.length()];
                bytesRead = fis.read(fileBytes, 0, (int) file.length());
                assert (bytesRead == fileBytes.length);
                assert (bytesRead == (int) file.length());
                fos.write(fileBytes);
                fos.flush();
                fileBytes = null;
                fis.close();
                fis = null;
            }
            fos.close();
            fos = null;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
