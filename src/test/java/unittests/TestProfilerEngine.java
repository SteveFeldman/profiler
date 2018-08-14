package unittests;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import test.kbay.AgentConfig;
import test.kbay.profiler.ProfilerEngine;
import test.kbay.util.Print;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Unit tests for ProfilerEngine
 * Note: Profiler engine is static. That is why only single test is
 *    possible unless you made a clean up call for ProfilerEngine
 */
public class TestProfilerEngine {

    @BeforeClass
    public static void initProfileEngine() {
        try {
            // For this case default settings are fine, even config is invalid
            AgentConfig config = new AgentConfig(null);
            config.shortOutput = true;

            // Note, this case doesn't cover memory because it doesn't worth to implement
            //  mock Instrumentation interface. Memory will be covered with integration tests.
            ProfilerEngine.init(config);
        }
        catch (Exception ex) {
            Assert.fail("Get an exception:\n" + Print.printExceptionStack(ex));
        }
    }

    @Test
    public void testProfilerEngine() {
        //
        try {
            ProfilerEngine.reset();

            // Empty results sisnce no action is happen
            Assert.assertTrue("Expected empty results", ProfilerEngine.getResults().size()==0 );

            // Empty result for action and no active tracking
            ProfilerEngine.processTrackingObject();
            Assert.assertTrue("Expected empty results", ProfilerEngine.getResults().size()==0 );

            // Simulate profiling
            // javax.servlet.http.HttpServletRequest argument case not checking. Can be done in intergration test only
            ProfilerEngine.startProfiling("my.test.function1", null );

            ProfilerEngine.processTrackingObject();
            processObjectFromDifferentThread();
            // Empty results are expected because profiling not finished yet
            Assert.assertTrue("Expected empty results", ProfilerEngine.getResults().size()==0 );

            ProfilerEngine.finishProfiling();

            List<String> res = ProfilerEngine.getResults();
            Assert.assertTrue("Expected single result", res.size()==1 );
            Assert.assertTrue( "Profiling result is wrong", res.get(0).equals("my.test.function1;1;0;2;0") );
        }
        catch (Exception ex) {
            Assert.fail("Get an exception:\n" + Print.printExceptionStack(ex));
        }
    }


    @Test
    public void stressTestProfilerEngine() {
        //
        try {
            ProfilerEngine.reset();

            final int threadsNum = 20;
            final int objPerThread = 100000;

            ArrayList<Thread> workers = new ArrayList<>();
            HashSet<String> funcSet = new HashSet<>();

            Semaphore threadStarter = new Semaphore(0);

            for (int t=0;t<threadsNum;t++) {
                final String funcName = "my.test.function_" + t;
                funcSet.add(funcName);
                workers.add(
                        new Thread() {
                            @Override
                            public void run() {
                                ProfilerEngine.startProfiling( funcName, null );
                                try {
                                    threadStarter.acquire(1);

                                    for (int r = 0; r < objPerThread; r++) {
                                        ProfilerEngine.processTrackingObject();
                                        if (r % (objPerThread / 10) == 1)
                                            Thread.sleep(1);
                                    }

                                    Thread.sleep(400); // wait everybody to finish
                                }
                                catch (InterruptedException ign) {
                                }
                                ProfilerEngine.finishProfiling();
                            }
                        }
                );
            }

            for (Thread th : workers) {
                th.start();
            }

            Thread.sleep(200);
            threadStarter.release(threadsNum);

            for (Thread th : workers) {
                th.join();
            }

            // Check the results.
            List<String> profRes = ProfilerEngine.getResults();
            Assert.assertTrue("Expected " + threadsNum  + " items in result", profRes.size()==threadsNum);
            for ( String profR : profRes ) {
                String [] profInfo = profR.split(";");
                Assert.assertTrue("Unable to parse profile info " + profR, profInfo.length==5);

                String funcName = profInfo[0];
                int threadObjNum = Integer.parseInt(profInfo[1]);
                int allObjNum = Integer.parseInt(profInfo[3]);

                Assert.assertTrue("Profiling results failed for method " + funcName, funcSet.remove(funcName));
                Assert.assertTrue("threadObjNum wrong value. Get " + threadObjNum, threadObjNum==objPerThread);
                Assert.assertTrue("allObjNum wrong value. Get " + allObjNum, allObjNum==objPerThread*threadsNum);
            }

        }
        catch (Exception ex) {
            Assert.fail("Get an exception:\n" + Print.printExceptionStack(ex));
        }
    }

    private void processObjectFromDifferentThread() throws InterruptedException {
        Thread thr = new Thread() {
            @Override
            public void run() {
                ProfilerEngine.processTrackingObject();
            }
        };
        thr.start();
        thr.join();
    }

}
