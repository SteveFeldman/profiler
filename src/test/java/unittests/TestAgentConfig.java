package unittests;

import org.junit.Assert;
import org.junit.Test;
import test.kbay.AgentConfig;
import test.kbay.util.ProfException;

/**
 * Unit tests for AgentConfig
 */
public class TestAgentConfig {
    @Test
    public void testAgentConfig() {
        { // no params case
            try {
                AgentConfig config = new AgentConfig(null);
                String valStr = config.validate();
                Assert.assertTrue( "Epmty config expected to be invalid", "Params error: Please specify functions that profiler need to track\n".equals(valStr));

            } catch (ProfException ex) {
                Assert.fail();
            }
        }

        { // wrong param format case
            try {
                new AgentConfig("incorectParam");
                Assert.fail();
            } catch (ProfException ex) {
                Assert.assertTrue( "incorectParam case", ex.getMessage().startsWith("Unable to parse config argument:") );
            }
        }

        { // incorrect param case
            try {
                new AgentConfig("logFile:myFile,unknownParam:value");
                Assert.fail();
            } catch (ProfException ex) {
                Assert.assertTrue( "incorectParam case", ex.getMessage().startsWith("Unknown config argument") && ex.getMessage().contains("unknownParam") );
            }

        }

        { // happy path with all params
            try {
                AgentConfig config = new AgentConfig("hist:11,obj2track:java.lang.String,obj2track:java.lang.Number,func2prof:my.test.method,func2prof:my.test.another,dispatch:custom.servlet.doDispatch,logFile:mylog,logLevel:Info,resultFile:myres");

                String valStr = config.validate();
                Assert.assertTrue("Validate expected to pass", valStr==null);

                String confVals = config.getParamsValueStr();
                String expectedConfVals = "hist:11\n" +
                        "obj2track 2 items:\n" +
                        "  java.lang.String\n" +
                        "  java.lang.Number\n" +
                        "func2prof 2 items:\n" +
                        "  my.test.method\n" +
                        "  my.test.another\n" +
                        "dispatch 1 items\n" +
                        "  custom.servlet.doDispatch\n" +
                        "logFile:mylog\n" +
                        "logLevel:Info\n" +
                        "resultFile:myres\n";

                Assert.assertTrue( "Config get correct values", expectedConfVals.equals(confVals) );

            } catch (ProfException ex) {
                Assert.fail();
            }

        }
    }
}
