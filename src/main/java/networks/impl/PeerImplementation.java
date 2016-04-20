package networks.impl;

import networks.models.RemotePeerInfo;
import networks.references.Constants;
import networks.utilities.LogHelper;

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
            LogHelper.getLogger().severe("FileNotFoundException" + Constants.Files.peerInfoFile);
        }
        try {
            String line;
            try {
                line = br.readLine();
                while (line != null) {
                    remotePeerInfoList.add(readRemotePeerInfoFromLine(line));
                    line = br.readLine();
                }
            } catch (IOException e) {
                LogHelper.getLogger().severe("IOException");
            }
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                LogHelper.getLogger().severe("IOException");
            }
        }

        return remotePeerInfoList;
    }

    public RemotePeerInfo readRemotePeerInfoFromLine(String line) {
        String[] tmp = line.split(" ");
        RemotePeerInfo remotePeerInfo = new RemotePeerInfo(Integer.parseInt(tmp[0]), tmp[1], Integer.parseInt(tmp[2]), (tmp[3].equals("1"))?true:false);
        return remotePeerInfo;
    }
}
