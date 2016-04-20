package utilities;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Formatter;
import java.util.logging.Level;

/**
 * Created by sank on 4/19/16.
 */
public class LOGGER {
    /**
     * Static instantiation of Logger.
     */
    private static final Logger logger = Logger.getRootLogger();

    /**
     * @return The current logging level.
     */
    public static String getLogLevel() {
        return logger.getLevel().toString();
    }

    /**
     * Reports at ERROR level.
     * @param message String message to append in log file.
     */
    public static void error(String message) {
        logger.error(message);
    }

    /**
     * Reports at WARN level.
     * @param message String message to append in log file.
     */
    public static void warn(String message) {
        logger.warn(message);
    }

    /**
     * Reports at INFO level.
     * @param message String message to append in log file.
     */
    public static void info(String message) {
        logger.info(message);
    }

    /**
     * Reports at DEBUG level.
     * @param message String message to append in log file.
     */
    public static void debug(String message) {
        logger.debug(message);
    }

    /**
     * Reports at TRACE level.
     * @param message String message to append in log file.
     */
    public static void trace(String message) {
        logger.trace(message);
    }

    /**
     * Reports at ERROR level.
     * @param message String message to append in log file.
     * @param throwable Caught exception to append in log file.
     */
    public static void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    /**
     * Reports at WARN level.
     * @param message String message to append in log file.
     * @param throwable Caught exception to append in log file.
     */
    public static void warn(String message, Throwable throwable) {
        logger.warn(message, throwable);
    }

    /**
     * Reports at INFO level.
     * @param message String message to append in log file.
     * @param throwable Caught exception to append in log file.
     */
    public static void info(String message, Throwable throwable) {
        logger.info(message, throwable);
    }

    /**
     * Reports at DEBUG level.
     * @param message String message to append in log file.
     * @param throwable Caught exception to append in log file.
     */
    public static void debug(String message, Throwable throwable) {
        logger.debug(message, throwable);
    }

    /**
     * Reports at TARCE level.
     * @param message String message to append in log file.
     * @param throwable Caught exception to append in log file.
     */
    public static void trace(String message, Throwable throwable) {
        logger.trace(message, throwable);
    }


//    public static void configure(int peerId)
//            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
//        PropertyFileUtility propertyFileUtility = new PropertyFileUtility("log4j.properties");
//        Handler handler = new FileHandler("log_peer_" + peerId + ".log");
////        Formatter formatter = (Formatter) Class.forName(propertyFileUtility.getStringValue("java.util.logging.FileHandler.formatter")).newInstance();
////        handler.setFormatter(formatter);
////        handler.setLevel(Level.parse(properties.getProperty("java.util.logging.FileHandler.level")));
//        logger.se
//    }
}
