package networks.impl.File;

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

    public void splitFile(File inputFile, int pieceSize){

        FileInputStream inputStream;
        String newFileName;
        FileOutputStream filePart;
        int fileSize = (int) inputFile.length();
        int nChunks = 0, read = 0, readLength = pieceSize;
        byte[] byteChunkPart;
        try {
            inputStream = new FileInputStream(inputFile);
            while (fileSize > 0) {
                if (fileSize <= 5) {
                    readLength = fileSize;
                }
                byteChunkPart = new byte[readLength];
                read = inputStream.read(byteChunkPart, 0, readLength);
                fileSize -= read;
                assert (read == byteChunkPart.length);
                nChunks++;
                newFileName = inputFile.getParent() + "/parts/" +
                        inputFile.getName() + "/" + Integer.toString(nChunks - 1);
                filePart = new FileOutputStream(new File(newFileName));
                filePart.write(byteChunkPart);
                filePart.flush();
                filePart.close();
                byteChunkPart = null;
                filePart = null;
            }
            inputStream.close();
        } catch (IOException e) {
            //todo: need to include warning
//            LogHelper.getLogger().warning(e);
        }
    }

    public void mergeFile() {
        //todo: why do we require these file names?
        File ofile = new File("ImageFile2.jpg");
        String FILE_NAME = "ImageFile.jpg";
        FileOutputStream fos;
        FileInputStream fis;
        byte[] fileBytes;
        int bytesRead = 0;
        List<File> list = new ArrayList<File>();
        for (int i = 0; i < 211; i++) {
            list.add(new File("parts/" + FILE_NAME + ".part" + i));
        }
        try {
            fos = new FileOutputStream(ofile, true);
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
        } catch (Exception e) {
            //todo:write logger here
//            LogHelper.getLogger().warning(e);
        }
    }
}
