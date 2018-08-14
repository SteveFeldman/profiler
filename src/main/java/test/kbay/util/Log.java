package test.kbay.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Profiler need to use custom simple logger becase we want to minimize number of included jars.
 * Logs need to be separated from app that we profile.
 *
 * Logs can go into the file or to the console. Logger not optimized for performance.
 * It is fine since it is prototyping.
 *
 * Default output: to console.
 * Default log level: DEBUG
 */
public class Log
{
    public final static int LEVEL_TRACE = 0;
    public final static int LEVEL_DEBUG = 1;
    public final static int LEVEL_INFO = 2;
    public final static int LEVEL_WARN = 3;
    public final static int LEVEL_ERROR = 4;

    private static int logLevel = LEVEL_TRACE; // LEVEL_DEBUG;

    // Logs will go to this file or will be printed on the console
    private static Path logFN = null;

    /**
     * setLogFileName - set logging target: file or console
     * @param name  - log file name. File will be clean up before usage. Null - console output
     * @param level - log level. Values: Err|Warn|Info|Debug|Trace
     */
    public static void setLogConfig(String name, String level) {
        if (name != null) {
            Path fn = Paths.get(name);

            try {
                Files.deleteIfExists(fn);
            } catch (Exception ex) {
                // Non fatal error. Just go with console
                System.err.println("Unable to setLogFileName for file " + fn + " Error:" + ex.getMessage());
                return;
            }
            logFN = fn;
        }

        if (level != null) {
            if ( "Err".equals(level) )
                logLevel = LEVEL_ERROR;
            else if ( "Warn".equals(level) )
                logLevel = LEVEL_WARN;
            else if ( "Info".equals(level) )
                logLevel = LEVEL_INFO;
            else if ( "Debug".equals(level) )
                logLevel = LEVEL_DEBUG;
            else if ( "Trace".equals(level) )
                logLevel = LEVEL_TRACE;
            else {
                System.err.println("Logging get unknown log level value '" + level + "'. Will use 'Err' level");
                logLevel = LEVEL_ERROR;
            }
        }

        info("setLogConfig to file " + logFN + " with log level " + logLevel);
    }

    /**
     * checkIfLog usefull to prevent building strings for logging.
     * @param level - LEVEL_XXXX value
     * @return true if this log level is active.
     */
    public static boolean checkIfLog( int level ) {
        return level>=logLevel;
    }

    public static void trace( String msg ) {
        if (logLevel <= LEVEL_TRACE )
            doLog(msg,"Trace", null);
    }

    public static void debug( String msg ) {
        if (logLevel <= LEVEL_DEBUG )
            doLog(msg,"Debug", null);
    }

    public static void info( String msg) {
        info(msg, null);
    }

    public static void info( String msg, Throwable ex ) {
        if (logLevel <= LEVEL_INFO )
            doLog(msg, "Info ", ex);
    }

    public static void warn( String msg) {
        warn(msg, null);
    }

    public static void warn( String msg, Throwable ex ) {
        if (logLevel < LEVEL_WARN )
            doLog(msg, "Warn ", ex);
    }

    public static void error( String msg) {
        error(msg, null);
    }

    public static void error( String msg, Throwable ex ) {
        if (logLevel < LEVEL_ERROR )
            doLog(msg, "Error", ex);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void doLog(String msg, String level, Throwable ex) {
        msg = Time.getCurrentTimeAsString() + " | " + level + " | " + msg;
        if (logFN==null) {
            System.err.println(msg);
            if (ex!=null)
                ex.printStackTrace();

            return;
        }

        //
        try {

            StringBuilder sb = new StringBuilder();
            sb.append(msg);
            if (ex!=null)
                sb.append( Print.printExceptionStack(ex) );
            sb.append("\n");
            Files.write( logFN, sb.toString().getBytes(),
                    StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND );
        }
        catch (IOException e) {
            // Non fatal error. Just go with console
            System.err.println("Unable to write log to the file. Switching to console. Error:" + ex.getMessage());
            logFN = null;
            doLog(msg, level, ex);
            return;
        }
    }
}
