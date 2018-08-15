package integrationtests;

import org.junit.Assert;
import org.junit.Test;
import testutil.Exec;
import test.kbay.util.Print;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Intergation tests with test App
 */
public class IntegrationTestApp {

    // Agent's short output parsing result
    private class MethodProfileResult {
        final String methodName;
        final int thrObjs; // Objects created in thread
        final int thrMem; // Memory allocated in thread
        final int allObjs; // Total objects created in thread.
        final int allMem; // Total memory allocaed in all threads

        MethodProfileResult(String methodName, int thrObjs, int thrMem, int allObjs, int allMem) {
            this.methodName = methodName;
            this.thrObjs = thrObjs;
            this.thrMem = thrMem;
            this.allObjs = allObjs;
            this.allMem = allMem;
        }
    }

    // Print usage test.
    @Test
    public void testUsage() {
        try {
            // Profiler instrumentation expected to be 0. So we will not be able to test
            List<String> lns = Exec.executeProfilerWithParams( "" );
            Assert.assertTrue("Expected long output with usage", lns.size() > 10);
            Assert.assertTrue("Expected usage output: " + lns, startWith(lns, "Usage:", 6));
        }
        catch (Exception ex) {
            Assert.fail("testProfiler failed due:\n" + Print.printExceptionStack(ex));
        }
    }

    // Respond with handler that was never active
    @Test
    public void testEmptyRes() {
        try {
            // Profiler instrumentation expected to be 0. So we will not be able to test
            List<String> lns = Exec.executeProfilerWithParams( "=func2prof:aaaa" );
            Assert.assertTrue("Expected some output", lns.size() > 10);
            Assert.assertTrue("Expected: Profiling agent is starting", startWith(lns,"Profiling agent is starting",6));

            Assert.assertTrue("Expected: 'Done' from app", equalsTo(lns, "Done") );
            Assert.assertTrue("Expected: Profiler result for last 0 items", equalsTo(lns, "Profiler result for last 0 items:"));
        }
        catch (Exception ex) {
            Assert.fail("testProfiler failed due:\n" + Print.printExceptionStack(ex));
        }
    }

    // Test with TestApp handlers that have different return policy.
    // Just want to be sure that finally block really do what is needed
    @Test
    public void testHandlerForEmptyApp() {
        testTestAppProfilingWith("app2test.TestHandlers.handler_0_ok", "java.lang.String", 2);
        testTestAppProfilingWith("app2test.TestHandlers.handler_3_ok", "java.lang.String", 2);
        testTestAppProfilingWith("app2test.TestHandlers.handler_0_ex", "java.lang.String", 2);
        testTestAppProfilingWith("app2test.TestHandlers.handler_0_ret", "java.lang.String", 2);

        testTestAppProfilingWith("app2test.TestHandlers.handler_0_ex", "java.lang.Long", 1);
        testTestAppProfilingWith("app2test.TestHandlers.handler_0_ex", "java.lang.Long,obj2track:java.lang.String", 3);
    }

    // Test if file output works
    @Test
    public void testFilesOutputs() {
        try {
            Files.deleteIfExists(Paths.get("agent.log"));
            Files.deleteIfExists(Paths.get("res.txt"));

            List<String> lns = Exec.executeProfilerWithParams(
                    "=func2prof:app2test.TestHandlers.handler_0_ok,obj2track:java.lang.Long,obj2track:java.lang.String,short:true,logFile:agent.log,logLevel:Info,resultFile:res.txt");
            List<MethodProfileResult> profRes = parseAgentShortOutput(lns);

            List<String> logs = Files.readAllLines( Paths.get("agent.log") );
            List<String> results = Files.readAllLines( Paths.get("res.txt") );

            Assert.assertTrue("Logs are found", logs!=null && logs.size()>=6);
            Assert.assertTrue("Result is found", results!=null && results.size()==1);

            // clean up after
            Files.deleteIfExists(Paths.get("agent.log"));
            Files.deleteIfExists(Paths.get("res.txt"));
        }
        catch (Exception ex) {
            Assert.fail("testProfiler failed due:\n" + Print.printExceptionStack(ex));
        }
    }


    ///////////////////////////////////////////////////////////////////////
    private void testTestAppProfilingWith(String methodName, String trackingObj, int expectedNumberOfObjects) {
        try {
            // Profiler instrumentation expected to be 0. So we will not be able to test
            List<String> lns = Exec.executeProfilerWithParams( "=func2prof:"+methodName+",obj2track:"+trackingObj+",short:true" );
            List<MethodProfileResult> profRes = parseAgentShortOutput(lns);

            Assert.assertTrue("Expected one result item", profRes!=null && profRes.size() == 1);
            MethodProfileResult res = profRes.get(0);
            Assert.assertTrue("Result method name", res.methodName.startsWith(methodName) );
            Assert.assertTrue("In thread objs", res.thrObjs==expectedNumberOfObjects );
            Assert.assertTrue("In thread objs", res.thrMem>100 );
            Assert.assertTrue("In thread objs", res.allObjs==expectedNumberOfObjects*4 );
            Assert.assertTrue("In thread objs", res.allMem>res.thrMem );
        }
        catch (Exception ex) {
            Assert.fail("testProfiler failed due:\n" + Print.printExceptionStack(ex));
        }
    }

    private boolean startWith( List<String> lines, String prefix, int limSz ) {
        for ( String s : lines ) {
            if (s.startsWith(prefix))
                return true;
            limSz--;
            if (limSz<=0)
                break;
        }
        return false;
    }

    private boolean equalsTo( List<String> lines, String str2search ) {
        for ( String s : lines ) {
            if (s.equals(str2search))
                return true;
        }
        return false;
    }

    /**
     * Parse Agent's short output. Expected that Log level is 'Err'
     * @param outputStrs - outptu from executeProfilerWithParams
     * @return Profile results. Return null in case of error.
     */
    private List<MethodProfileResult> parseAgentShortOutput(List<String> outputStrs) {
        // search for 'Profiler result for '
        Iterator<String> outI = outputStrs.iterator();

        while(outI.hasNext()) {
            if (outI.next().startsWith("Profiler result for "))
                break;
        }

        if (!outI.hasNext())
            return null;

        List<MethodProfileResult> res = new ArrayList<>();

        while(outI.hasNext()) {
            String profShortStr = outI.next();
            if (profShortStr.isEmpty())
                continue;
            // Example: app2test/TestHandlers.handler_3_ok;2;1176;8;3536
            String [] profRes = profShortStr.split(";");
            if (profRes.length!=5)
                continue;

            res.add( new MethodProfileResult(profRes[0],
                    Integer.parseInt(profRes[1]), Integer.parseInt(profRes[2]),
                    Integer.parseInt(profRes[3]), Integer.parseInt(profRes[4])) );
        }
        return res;
    }
}
