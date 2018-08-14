package test.kbay.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Simple printing tools
 */
public class Print {

    public static String strArr2Str( String [] sa ) {
        StringBuilder sb = new StringBuilder();

        sb.append("[");
        for (String s : sa) {
            if (sb.length()>1)
                sb.append(",");
            sb.append(s);
        }
        sb.append("]");

        return sb.toString();
    }

    public static String printExceptionStack(Throwable ex)
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        ex.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
