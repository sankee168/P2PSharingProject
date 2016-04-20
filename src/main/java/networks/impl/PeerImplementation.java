package networks.impl;

import networks.models.RemotePeerInfo;
import networks.references.Constants;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by sank on 4/19/16.
 */
public class PeerImplementation {

    public List<RemotePeerInfo> getAllPeerInfo() {
        BufferedReader br = null;
        List<RemotePeerInfo> remotePeerInfoList = new LinkedList<RemotePeerInfo>();

        try {
            br = new BufferedReader(new FileReader(Constants.Files.peerInfoFile));
        } catch (FileNotFoundException e) {
            //todo: handle this exception
            e.printStackTrace();
        }
        try {
            StringBuilder sb = new StringBuilder();
            String line = null;
            try {
                line = br.readLine();
                while (line != null) {
//                    sb.append(line);
//                    sb.append(System.lineSeparator());
//                    line = br.readLine();
//                    String everything = sb.toString();
                    remotePeerInfoList.add(readRemotePeerInfoFromLine(line));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return remotePeerInfoList;
    }

    public RemotePeerInfo readRemotePeerInfoFromLine(String line) {
        String[] tmp = line.split(" ");
        RemotePeerInfo remotePeerInfo = new RemotePeerInfo(Integer.parseInt(tmp[0]), tmp[1], Integer.parseInt(tmp[2]), Boolean.getBoolean(tmp[3]));
        return remotePeerInfo;
    }
}
