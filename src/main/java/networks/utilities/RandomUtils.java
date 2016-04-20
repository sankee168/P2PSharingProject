package networks.utilities;

import networks.models.RemotePeerInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sank on 4/20/16.
 */
public class RandomUtils {
    public Set<Integer> getIds(Collection<RemotePeerInfo> peers) {
        Set<Integer> ids = new HashSet<Integer>();
        for (RemotePeerInfo peer : peers) {
            ids.add(peer.getPeerId());
        }
        return ids;
    }
}
