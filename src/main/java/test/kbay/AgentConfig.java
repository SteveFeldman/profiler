package test.kbay;

import test.kbay.util.ProfException;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Agent config data. Here is we keep all constats and
 * read from agent argument data
 */
public class AgentConfig {

    // short output usefull for testing
    // This flag is not documented
    public boolean shortOutput = false;

    // History of stored profile calls
    public int profileHistoryLen = 50;

    // Object constructors that we are tracking
    public Set<String> object2track = new HashSet<>();
    // Function to profile. For Spring it is org.springframework.web.servlet.DispatcherServlet.doDispatch
    public Set<String> funcNames2profile = new HashSet<>();

    // Dispatcher function name to show results in the browser
    public Set<String>  functNameDispatcher = new HashSet<>();

    // Log file name. If null, console will be used.
    public String logFn;
    // Log level
    public String logLevel = "Err";// "Debug";
    // Result file name. If null, console will be used.
    public String resFn;

    /**
     * AgentConfig parse config data. Please call validate method after to check if all necessary data is available.
     * @param agentArgs  - javaagent agrument string
     * @throws ProfException
     */
    public AgentConfig( String agentArgs ) throws ProfException {

        String [] arglist;

        if (agentArgs==null || agentArgs.length()==0)
            arglist = new String[0];
        else
            arglist = agentArgs.split(",");

        for (String arg : arglist) {
            arg = arg.trim();
            if (arg.isEmpty())
                continue;

            int idx = arg.indexOf(':');
            if (idx<=0)
                throw new ProfException("Unable to parse config argument: '" + arg + "'");

            String key = arg.substring(0,idx).trim();
            String value = arg.substring(idx+1).trim();
            if (value.isEmpty())
                throw new ProfException("Unable to parse config argument: '" + arg + "'");

            if ( "hist".equals(key) ) {
                try {
                    profileHistoryLen = Integer.parseInt(value);
                }
                catch (Exception ex) {
                    throw new ProfException("Unable to parse config argument: '" + arg + "', expected integer value");
                }
            }
            else if ("obj2track".equals(key)) {
                object2track.add(value);
            }
            else if ("func2prof".equals(key)) {
                funcNames2profile.add(value);
            }
            else if ("dispatch".equals(key)) {
                functNameDispatcher.add(value);
            }
            else if ("logFile".equals(key)) {
                logFn = value;
            }
            else if ("resultFile".equals(key)) {
                resFn = value;
            }
            else if ("logLevel".equals(key)) {
                logLevel = value;
            }
            else if ("short".equals(key)) {
                shortOutput = "true".equalsIgnoreCase(value);
            }
            else {
                throw new ProfException("Unknown config argument: '" + arg + "'");
            }
        }

        if (functNameDispatcher.size()==0)
            functNameDispatcher.add("org.springframework.web.servlet.DispatcherServlet.doDispatch");
    }

    /**
     * Usage info for params
     * @return string with usage info
     */
    public static String getParamsInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("All parameters are comma separated (','). objCtor and func2prof can be included multiple times\n");
        sb.append("Example: objCtor=java.lang.String;objCtor=java.lang.Number;func2prof=my.test.method\n");
        sb.append("  hist:50                    - how many calls tp profiles funtions profiler keeps in the history\n");
        sb.append("  obj2track:java.lang.String - object usage that need to be profiled\n");
        sb.append("  func2prof:my.test.method   - method that we need to profile. If method\n");
        sb.append("                             has argument javax.servlet.http.HttpServletRequest, profile call will be \n");
        sb.append("                             updated with url\n");
        sb.append("  dispatch:org.springframework.web.servlet.DispatcherServlet.doDispatch - Dispatch method for your webserver.\n");
        sb.append("                             If this method exist, you will get results on a webpage with any url that ends with '/profiler'\n");
        sb.append("  logFile:<file name>        - Logs output to file insted of console\n");
        sb.append("  logLevel:Err|Warn|Info|Debug|Trace - Logs level. Default is Err\n");
        sb.append("  resultFile:<file name>     - Result output to file instead of console\n");
        return sb.toString();
    }

    /**
     * Validate params.
     * @return  null in case of success. String with error - in case of failure
     */
    public String validate() {
        StringBuilder res = new StringBuilder();
        // No objects to track make sence since there is memory tracking exist
        //if ( object2track.size() == 0 )
        //    res.append("Params error: Please specify constructors of objects that profiler need to track\n");

        if ( funcNames2profile.size() == 0 )
            res.append("Params error: Please specify functions that profiler need to track\n");

        if (res.length()==0)
            return null;

        return res.toString();
    }

    /**
     * Print param values into the string
     * @return printed values
     */
    public String getParamsValueStr() {
        StringBuilder sb = new StringBuilder();
        sb.append("hist:"+profileHistoryLen+"\n");
        sb.append("obj2track "+ object2track.size() +" items:\n");
        for (String s : object2track) {
            sb.append("  "+ s + "\n");
        }

        sb.append("func2prof "+ funcNames2profile.size() +" items:\n");
        for (String s : funcNames2profile) {
            sb.append("  "+ s + "\n");
        }
        sb.append("dispatch "+functNameDispatcher.size()+" items\n");
        for (String s : functNameDispatcher) {
            sb.append("  " + s + "\n");
        }

        sb.append("logFile:"+(logFn==null?"CONSOLE":logFn)+"\n");
        sb.append("logLevel:"+(logLevel==null?"Default":logLevel)+"\n");
        sb.append("resultFile:"+(resFn==null?"CONSOLE":resFn)+"\n");

        return sb.toString();
    }
}


