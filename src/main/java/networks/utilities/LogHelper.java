package networks.utilities;

import networks.models.RemotePeerInfo;

import java.io.*;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.*;

public class LogHelper {
    private static final String CONF = "log4j.properties";
    private static final LogHelper _log = new LogHelper(Logger.getLogger("CNT5106C"));

    static {
        InputStream in = null;
        try {
            in = LogHelper.class.getClassLoader()
                    .getResourceAsStream(CONF);
//            in = LogHelper.class.getResourceAsStream(CONF);
            LogManager.getLogManager().readConfiguration(in);
        } catch (IOException e) {
            System.err.println(LogHelper.stackTraceToString(e));
            System.exit(1);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    private final Logger _l;

    private LogHelper(Logger log) {
        _l = log;
    }

    public static void configure(int peerId)
            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        Properties properties = new Properties();
        properties.load(LogHelper.class.getClassLoader().getResourceAsStream(CONF));
        Handler handler = new FileHandler("log_peer_" + peerId + ".log");
        Formatter formatter = (Formatter) Class.forName(properties.getProperty("java.util.logging.FileHandler.formatter")).newInstance();
        handler.setFormatter(formatter);
        handler.setLevel(Level.parse(properties.getProperty("java.util.logging.FileHandler.level")));
        _log._l.addHandler(handler);
    }

    public static LogHelper getLogger() {
        return _log;
    }

    public static String getPeerIdsAsString2(Collection<Integer> peersIDs) {
        StringBuilder sb = new StringBuilder("");
        boolean isFirst = true;
        for (Integer peerId : peersIDs) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(", ");
            }
            sb.append(peerId.intValue());
        }
        return sb.toString();
    }

    public static String getPeerIdsAsString(Collection<RemotePeerInfo> peers) {
        StringBuilder sb = new StringBuilder("");
        boolean isFirst = true;
        for (RemotePeerInfo peer : peers) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(", ");
            }
            sb.append(peer.getPeerId());
        }
        return sb.toString();
    }

    public synchronized void conf(String msg) {
        _l.log(Level.CONFIG, msg);
    }

    public synchronized void debug(String msg) {
        _l.log(Level.FINE, msg);
    }

    public synchronized void info(String msg) {
        _l.log(Level.INFO, msg);
    }

    public synchronized void severe(String msg) {
        _l.log(Level.SEVERE, msg);
    }

    public synchronized void warning(String msg) {
        _l.log(Level.WARNING, msg);
    }

    public synchronized void severe(Throwable e) {
        _l.log(Level.SEVERE, stackTraceToString(e));
    }

    public synchronized void warning(Throwable e) {
        _l.log(Level.WARNING, stackTraceToString(e));
    }

    private static String stackTraceToString(Throwable t) {
        final Writer sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
