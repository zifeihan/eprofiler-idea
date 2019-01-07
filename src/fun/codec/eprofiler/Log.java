package fun.codec.eprofiler;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Date;

/**
 * @author Rastislav Papp (rastislav.papp@gmail.com)
 */
public class Log {

    public enum Level {
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4),
        FATAL(5);

        private int order;

        Level(int order) {
            this.order = order;
        }

        public boolean includes(Level level) {
            return this.order <= level.order;
        }

    }

    private static Level LEVEL = Level.INFO;
    private static PrintStream OUTPUT = System.out;

    public static void fatal(String message, Object... params) {
        fatal(message, null, params);
    }

    public static void fatal(String message, Throwable throwable, Object... params) {
        log(Level.FATAL, message, throwable, params);
    }

    public static void error(String message, Object... params) {
        error(message, null, params);
    }

    public static void error(String message, Throwable throwable, Object... params) {
        log(Level.ERROR, message, throwable, params);
    }

    public static void warn(String message, Object... params) {
        warn(message, null, params);
    }

    public static void warn(String message, Throwable throwable, Object... params) {
        log(Level.WARN, message, throwable, params);
    }

    public static void info(String message, Object... params) {
        info(message, null, params);
    }

    public static void info(String message, Throwable throwable, Object... params) {
        log(Level.INFO, message, throwable, params);
    }

    public static void debug(String message, Object... params) {
        debug(message, null, params);
    }

    public static void debug(String message, Throwable throwable, Object... params) {
        log(Level.DEBUG, message, throwable, params);
    }

    private static void log(Level level, String message, Throwable throwable, Object... params) {
        if (LEVEL.includes(level)) {
            String formattedMessage = MessageFormat.format(message, params);
            OUTPUT.println(MessageFormat.format("{0,date,yyyy-MM-dd HH:mm:ss:SSS} [{1}] Panda Profiler Agent: {2}",
                    new Date(), level.name(), formattedMessage));
            if (throwable != null) {
                throwable.printStackTrace(OUTPUT);
            }
        }
    }

    public static void setLevel(Level level) {
        Log.LEVEL = level;
    }

}
