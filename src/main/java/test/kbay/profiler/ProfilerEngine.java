package test.kbay.profiler;

import test.kbay.AgentConfig;
import test.kbay.util.Log;
import test.kbay.util.Reflection;
import test.kbay.util.Time;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Global profiler engine.
 * All calls are concurrent friendly.
 */
public class ProfilerEngine {
    // What is in the process of profiling
    private static final ConcurrentHashMap<ProfileContext, ProfileContext> activeContext; // Key: thread Id. Value list of method to profile
    private static final ConcurrentLinkedQueue<ProfileContext> historyContext; // Profile history. History data is end result for the user.

    // Stack per thread. Key: Thread hash, Value context stack
    private static final ConcurrentHashMap<Integer, LinkedList<ProfileContext>> contextStack = new ConcurrentHashMap<>();

    // might be null during tests
    private static AgentConfig config;

    static {
        activeContext = new ConcurrentHashMap<>();
        historyContext = new ConcurrentLinkedQueue<>();
    }

    /**
     * init must be call
     *
     * @param conf - agent configuration
     */
    public static void init(AgentConfig conf) {
        config = conf;
    }

    /**
     * Reset internal contexts. Used in QA tests
     */
    public static void reset() {
        activeContext.clear();
        historyContext.clear();
    }

    /**
     * startProfiling - entry point for profiling. Start profiling.
     *
     * @param name               - name of profile call (name of profiled method)
     * @param httpServletRequest - optional Request that can be used to name context.
     * @return profile context. Will be used for finishProfiling
     */
    public static void startProfiling(String name, Object httpServletRequest) {
        // Creating friendly name for the context

        String profileName = buildProfileName(name, httpServletRequest);

        ProfileContext context = new ProfileContext(profileName);

        activeContext.put(context, context);

        Integer thrId = Thread.currentThread().hashCode();
        LinkedList<ProfileContext> stack = contextStack.get(thrId);
        if (stack == null) {
            stack = new LinkedList<>();
            contextStack.put(thrId, stack);
        }
        synchronized (stack) {
            stack.add(context);
        }

        if (Log.checkIfLog(Log.LEVEL_INFO))
            Log.info("Create context: " + context);
    }

    /**
     * finishProfiling - entry point for profiling. Finish profiling and make results available.
     */
    public static void finishProfiling() {
        Integer thrId = Thread.currentThread().hashCode();
        LinkedList<ProfileContext> stack = contextStack.get(thrId);
        if (stack == null) {
            Log.error("finishProfiling not found stack for therad id " + thrId);
            return;
        }

        ProfileContext context = null;
        synchronized (stack) {
            context = stack.pollLast();

            if (stack.size() == 0)
                contextStack.remove(thrId);
        }

        if (context == null) {
            Log.error("finishProfiling found empty stack for therad id " + thrId);
            return;
        }

        context.onFinishProcessing();

        activeContext.remove(context);

        // History size is critical. No need to lock
        while (historyContext.size() >= config.profileHistoryLen)
            historyContext.poll();

        historyContext.add(context);

        printResultStr( context.getResultStr( config.shortOutput ) );

        if (Log.checkIfLog(Log.LEVEL_INFO))
            Log.info("Finish to process a context: " + context);
    }

    /**
     * Patch that provides profiling data for HTTP server. If request ends with 'profiler', the profile result data will be
     *    returned to caller.
     * @param httpServletRequest
     * @param httpServletResponse
     * @return  return true if profile data was returned.
     */
    public static boolean onDispatcherRequest( Object httpServletRequest, Object httpServletResponse ) {
        if (httpServletRequest == null || httpServletResponse == null) {
            Log.info("onDispatcherRequest call with null arg values");
            return false;
        }

        Log.debug("onDispatcherRequest called with httpServletRequest: " + httpServletRequest.getClass().getName() + " and response " + httpServletResponse.getClass().getName() );
        Method getRequestURLMethod = Reflection.findMethodByName( httpServletRequest, "getRequestURL" );
        if (getRequestURLMethod==null) {
            Log.warn("onDispatcherRequest fail to process httpServletRequest argument");
            return false;
        }

        try {
            Object url = getRequestURLMethod.invoke(httpServletRequest);
            Log.debug("onDispatcherRequest called with url: " + url);
            if (url == null || !url.toString().endsWith("profiler")) {
                return false;
            }
        }
        catch (Exception ex) {
            Log.error("onDispatcherRequest reflection error", ex);
            return false;
        }

        // We don't checking http method, can be any.
        // Return the result for this request
        Method setStatusMethod = Reflection.findMethodByName( httpServletResponse, "setStatus" );
        Method getWriterMethod = Reflection.findMethodByName( httpServletResponse, "getWriter" );

        if (setStatusMethod==null || getWriterMethod==null) {
            Log.warn("onDispatcherRequest fail to process httpServletResponse argument");
            return false;
        }

        try {
            setStatusMethod.invoke(httpServletResponse, 200);
            PrintWriter writer = (PrintWriter)getWriterMethod.invoke(httpServletResponse);

            List<String> profResults = getResults();

            writer.println("Profiler result for last " + profResults.size() + " items:");
            for (String prRes : profResults) {
                writer.println(prRes);
            }
            // Done writing, let's flush the data
            writer.flush();
            return true;
        }
        catch (Exception ex) {
            Log.error("onDispatcherRequest reflection error", ex);
            return false;
        }

    }

    /**
     * Entry point for tracking Object feed. We don't keep tracking for object type even it is easy to add.
     */
    public static void processTrackingObject() {

        if (activeContext.isEmpty())
            return; // no contexts to update

        int thrHash = Thread.currentThread().hashCode();

        for (ProfileContext cont : activeContext.values()) {
            cont.onObjectTracking(thrHash);
        }
    }

    /**
     * Entry point for tracking memory usage.
     *
     * @param memUsage - memory usage
     */
    public static void processmemoryUsage(long memUsage) {

        if (activeContext.isEmpty())
            return; // no contexts to update

        int thrHash = Thread.currentThread().hashCode();

        for (ProfileContext cont : activeContext.values())
            cont.onMemoryTracking(thrHash, memUsage);
    }

    /**
     * print profiling history where config say: Console or file
     */
    public static void printResults() {
        StringBuilder res2print = new StringBuilder();

        List<String> res = getResults();
        res2print.append("Profiler result for last " + res.size() + " items:\n");
        for (String r : res) {
            res2print.append(r + "\n");
        }

        printResultStr(res2print.toString());
    }

    /**
     * print results as a list. Needed for requests by demand
     */
    public static List<String> getResults() {
        List<String> res = new LinkedList<>();
        for (ProfileContext prCont : historyContext) {
            res.add(prCont.getResultStr(config.shortOutput));
        }
        return res;
    }


    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Generate name for profiled item
     * @param name  - name of prifiled item
     * @param httpServletRequest  - optional request. Will be used to extract URL
     * @return name for profile
     */
    private static String buildProfileName(String name, Object httpServletRequest) {
        StringBuilder profName = new StringBuilder();
        if (!config.shortOutput)
            profName.append(Time.getCurrentTimeAsString()).append(" ");

        name = name.replace('/','.'); // method name

        // Check the args. If we see HTTP Request
        if (httpServletRequest != null) {
            try {
                Log.debug("buildProfileName called with httpServletRequest class: " + httpServletRequest.getClass().getName() );

                // Here we can't just do httpServletRequest.getQueryString() because class location is unknown and we will end up
                // with java.lang.NoClassDefFoundError. So we have to enumerate the methods

                Method getRequestURLMethod = Reflection.findMethodByName( httpServletRequest, "getRequestURL" );
                if (getRequestURLMethod!=null) {
                    Object url = getRequestURLMethod.invoke(httpServletRequest);
                    Log.debug("buildProfileName found caller url: " + url );
                    // Substitute the method name with URL if available. Printing both method name and URL looks ugly.
                    if (url!=null)
                        name = url.toString();
                }
                else {
                    Log.warn("Failed to find method for HttpRequest");
                }

            } catch (Throwable ex) {
                Log.warn("Unable to enrich profile name due an error: " + ex.getMessage(), ex);
            }
        }

        if (name != null || name.length() == 0)
            profName.append(name);

        return profName.toString();
    }

    // Note:  synchronized might be a bottleneck if there are huge number of calls in profiling.
    //      So far it is not our case, will work fine.
    private static synchronized void printResultStr(String str) {
        PrintStream output = System.err;

        if (config != null && config.resFn != null) {
            try {
                output = new PrintStream(
                        new FileOutputStream(config.resFn, false));
            } catch (Throwable ex) {
                Log.error("Unable to create file for result printing", ex);
            }
        }

        output.println(str);

        // close if it is file
        if (output != System.err)
            output.close();
    }
}