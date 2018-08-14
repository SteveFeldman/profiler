package test.kbay.util;

/**
 * Profiler exception class for internal errors.
 */
public class ProfException extends Exception {
    public ProfException(String message) {
        super(message);
    }

    public ProfException(String message, Throwable cause) {
        super(message, cause);
    }
}
