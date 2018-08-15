package testutil;

import test.kbay.util.Print;
import unittests.TestConfig;
import test.kbay.util.Log;
import test.kbay.util.ProfException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line executor.
 * Note: can be used for simple tests only, for production
 *    implementation must be more complicated to be tolerant
 *    to Streams issues
 * Note: Timeout implementation has a potential for resource leaking. Acceptable for QA tests only.
 */
public class Exec {

    /**
     * Execute profiler with Test App.
     * @param params  - profiler argument line
     * @return  err output lines
     * @throws ProfException
     */
    public static List<String> executeProfilerWithParams(String params ) throws ProfException {
        return Exec.execute(30000, new String[] {"java", "-javaagent:lib/java-allocation-instrumenter-3.1.0.jar",  "-javaagent:"+ TestConfig.profilerJarLocation+params,
                "-cp", "target/test-classes", "app2test.TestApp"}).output;
    }

    /**
     * Execute curl commmand and return output
     * @param url  - Url for curl
     * @return output lines
     * @throws ProfException
     */
    public static List<String> executeCurl(String url ) throws ProfException {
        return Exec.execute(30000, new String[] {"curl","-sS", url}).output;
    }


    /**
     * Execute profiler with WebGoat. Profiling will be done for 'String'
     * @return Process instance to kill the webgoat afther test.
     */
    public static Process executeSpringApp() {
        // 5 minutes is enough to finish the tests.

        try {
            return Exec.execute(0, new String[] {"java", "-javaagent:lib/java-allocation-instrumenter-3.1.0.jar",
                    "-javaagent:target/profiler-1.0.jar=func2prof:org.springframework.web.servlet.DispatcherServlet.doDispatch,obj2track:java.lang.String,logLevel:Debug,logFile:springAgentLogs.txt,resultFile:profiler.txt",
                    "-jar", "test_lib/webgoat-server-8.0.0.M21.jar"}).proc;
        }
        catch (Exception e) {
            Log.error("Failed to start Webgoat server",e);
            return null;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////

    private static class ExecResult {
        final Process proc;
        final List<String> output;

        ExecResult(Process proc, List<String> output) {
            this.proc = proc;
            this.output = output;
        }
    }

    /**
     * Execite app and return the output
     * @param timeoutMs - process run timeout in ms.
     * @param cmdArr - command to execute
     * @return result of executiom
     */
    private static ExecResult execute(long timeoutMs,  String [] cmdArr ) throws ProfException {

        Log.info("Executing " + Print.strArr2Str(cmdArr));

        final ArrayList<String> res = new ArrayList<>();

        try {
            Process process = Runtime.getRuntime().exec(cmdArr);

            Thread runThr1 = new Thread() {
                @Override
                public void run() {
                    setName("app "+cmdArr[0] + " out reading");
                    BufferedReader stdIn = new BufferedReader(new InputStreamReader( process.getInputStream() ));

                    try {
                        String s;
                        while ((s = stdIn.readLine()) != null) {
                            res.add(s);
                            //Log.info( "Out: " + s );
                        }
                    }
                    catch (Exception ex) {
                        Log.error("execute failed to read output for the process: " + Print.strArr2Str(cmdArr), ex);
                    }
                }
            };
            runThr1.start();

            Thread runThr2 = new Thread() {
                @Override
                public void run() {
                    setName("app "+cmdArr[0] + " err reading");
                    BufferedReader stdIn = new BufferedReader(new InputStreamReader( process.getErrorStream() ));

                    try {
                        String s;
                        while ((s = stdIn.readLine()) != null) {
                            res.add(s);
                            //Log.info( "Err: " + s );
                        }
                    }
                    catch (Exception ex) {
                        Log.error("execute failed to read output for the process: " + Print.strArr2Str(cmdArr), ex);
                    }
                }
            };
            runThr2.start();

            if (timeoutMs>0) {
                runThr1.join(timeoutMs);
                runThr2.join(timeoutMs);
            }
            return new ExecResult(process, res);
        }
        catch (Exception ex) {
            throw new ProfException("Failed to execute: " + Print.strArr2Str(cmdArr), ex);
        }


    }

}
