package references;

/**
 * Created by sank on 4/19/16.
 */
public class Constants {
    static public class HandShakeHeader {
        public static final String headerName = "P2PFILESHARINGPROJ";
        public static final int ZERO_BIT_SIZE = 10;
        public static final int PEER_ID_BIT_SIZE = 4;
        public static final String CHAR_SET = "US-ASCII";
        public static final String CONFIG_FILE = "Common.cfg";
        private static final String COMMENT = "#";
    }

    static public class CommonConfig {
        public static final String numberOfPreferredNeighbours = "NumberOfPreferredNeighbors";
        public static final String unChokingInterval = "UnchokingInterval";
        public static final String optimisticUnchokingInterval = "OptimisticUnchokingInterval";
        public static final String fileName = "FileName";
        public static final String fileSize = "FileSize";
        public static final String pieceSize = "PieceSize";
    }

}
