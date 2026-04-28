package android.util;

import java.util.logging.Logger;

/**
 * Stub implementation of android.util.Log for JVM/Desktop builds.
 * Delegates to java.util.logging.Logger so the libdecsync library works on JVM.
 */
public class Log {
    private static final Logger logger = Logger.getLogger("DecSync");

    public static int d(String tag, String msg) {
        logger.fine("[" + tag + "] " + msg);
        return 0;
    }

    public static int d(String tag, String msg, Throwable tr) {
        logger.fine("[" + tag + "] " + msg + " " + tr);
        return 0;
    }

    public static int i(String tag, String msg) {
        logger.info("[" + tag + "] " + msg);
        return 0;
    }

    public static int i(String tag, String msg, Throwable tr) {
        logger.info("[" + tag + "] " + msg + " " + tr);
        return 0;
    }

    public static int w(String tag, String msg) {
        logger.warning("[" + tag + "] " + msg);
        return 0;
    }

    public static int w(String tag, String msg, Throwable tr) {
        logger.warning("[" + tag + "] " + msg + " " + tr);
        return 0;
    }

    public static int e(String tag, String msg) {
        logger.severe("[" + tag + "] " + msg);
        return 0;
    }

    public static int e(String tag, String msg, Throwable tr) {
        logger.severe("[" + tag + "] " + msg + " " + tr);
        return 0;
    }

    public static int v(String tag, String msg) {
        logger.finest("[" + tag + "] " + msg);
        return 0;
    }

    public static int v(String tag, String msg, Throwable tr) {
        logger.finest("[" + tag + "] " + msg + " " + tr);
        return 0;
    }

    public static int wtf(String tag, String msg) {
        logger.severe("[WTF][" + tag + "] " + msg);
        return 0;
    }

    public static int wtf(String tag, String msg, Throwable tr) {
        logger.severe("[WTF][" + tag + "] " + msg + " " + tr);
        return 0;
    }

    public static boolean isLoggable(String tag, int level) {
        return true;
    }

    public static String getStackTraceString(Throwable tr) {
        if (tr == null) return "";
        java.io.StringWriter sw = new java.io.StringWriter();
        tr.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    public static final int ASSERT = 7;
}
