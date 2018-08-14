package test.kbay.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Time related tools
 */
public class Time {
    static final private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * @return formatted current time
     */
    public static String getCurrentTimeAsString() {
        return dateFormat.format(new Date());
    }

}
