package networks.impl.File;

import networks.utilities.LogHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sank on 4/20/16.
 */
//todo: need to edit this file completely
public class FileOperations {

    private File file;
    private File chunksDir;
    public FileOperations(File file, File chunksDir){
        this.file = file;
        this.chunksDir = chunksDir;
    }

    public void splitFile(int pieceSize){

        FileInputStream inputStream;
        String newFileName;
        FileOutputStream filePart;
        int fileSize = (int) file.length();
        int nChunks = 0, read = 0, readLength = pieceSize;
        byte[] byteChunkPart;
        try {
            inputStream = new FileInputStream(file);
            while (fileSize > 0) {
                if (fileSize <= 5) {
                    readLength = fileSize;
                }
                byteChunkPart = new byte[readLength];
                read = inputStream.read(byteChunkPart, 0, readLength);
                fileSize -= read;
                assert (read == byteChunkPart.length);
                nChunks++;
                newFileName = file.getParent() + "/parts/" +
                        file.getName() + "/" + Integer.toString(nChunks - 1);
                filePart = new FileOutputStream(new File(newFileName));
                filePart.write(byteChunkPart);
                filePart.flush();
                filePart.close();
            }
            inputStream.close();
        } catch (IOException e) {
            //todo: need to include warning
            LogHelper.getLogger().warning(e);
        }
    }

    public void mergeFile(int numParts) {
        File ofile = file;
        FileOutputStream fos;
        FileInputStream fis;
        byte[] fileBytes;
        int bytesRead = 0;
        List<File> list = new ArrayList<File>();
        for (int i = 0; i < numParts; i++) {
            list.add(new File(chunksDir.getPath() + "/" + i));
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
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
