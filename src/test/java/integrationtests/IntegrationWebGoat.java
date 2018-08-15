package integrationtests;

import org.junit.Assert;
import org.junit.Test;
import test.kbay.util.Log;
import testutil.Exec;
import test.kbay.util.Print;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Integration test for Web Goat. Note, webserver start will take around 1 minute, please
 * don't abuse the system
 */
public class IntegrationWebGoat {
    /**
     * integration test with Spring base app. Here we are using WebGoat: https://github.com/WebGoat/WebGoat
     */
    @Test
    public void testSpringApp() {
        Process springProc = null;
        try {
            Files.deleteIfExists(Paths.get("springAgentLogs.txt"));
            Files.deleteIfExists(Paths.get("profiler.txt"));
            springProc = Exec.executeSpringApp();
            Assert.assertTrue("Failed to start spring app", springProc!=null);

            // wait for the app 90 seconds
            for (int i=0;i<90;i++) {
                Thread.sleep(1000);
                if (Exec.executeCurl("http://localhost:8080").size()==0) {
                    Log.info("Webgoat is running");
                    break;
                }
            }

            Assert.assertTrue("Unable to start webapp", Exec.executeCurl("http://localhost:8080").size()==0);

            Thread.sleep(3000);

            // 2 calls are expecting
            Exec.executeCurl("http://localhost:8080/WebGoat/js/goatApp/support/GoatUtils.js");
            Exec.executeCurl("http://localhost:8080/WebGoat/login");
            // Because of securuty URL might fail. Let' use a file instead
            //List<String> profileResult = Exec.executeCurl("http://localhost:8080/WebGoat/css/profiler");

            List<String> profileResult = Files.readAllLines(Paths.get("profiler.txt"));

            if (profileResult.size()!=2){
                // it is failure, let's print logfile first
                List<String> logLns = Files.readAllLines(Paths.get("springAgentLogs.txt"));
                Log.info("Spring profile output has " + logLns.size() + " lines:");
                for (String l : logLns) {
                    Log.info(l);
                }
            }

            Assert.assertTrue("Profile results expected", profileResult.size()==2);
            Assert.assertTrue("Not found request 1", profileResult.get(1).contains("http://localhost:8080/WebGoat/js/goatApp/support/GoatUtils.js") );
            Assert.assertTrue("Not found request 2", profileResult.get(2).contains("http://localhost:8080/WebGoat/login") );
        }
        catch (Exception ex) {
            Assert.fail("testProfiler failed due:\n" + Print.printExceptionStack(ex));
        }
        finally {
            // Terminate web app
            if (springProc!=null) {
                springProc.destroy();
            }

        }
    }

}
