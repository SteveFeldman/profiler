package test.kbay;

import test.kbay.profiler.ProfilerEngine;
import test.kbay.util.Log;

import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import com.google.monitoring.runtime.instrumentation.Sampler;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Profiler Agent - starting [point for the whole Agent
 */
public class Agent {

    public static void premain(String args, Instrumentation instrumentation) {
        // Parsing arguments to build AgentConfig
        if (args == null || args.isEmpty()) {
            printUsage();
            System.exit(2);
        }
        AgentConfig config = null;
        try {
            config = new AgentConfig(args);
            String validStr = config.validate();
            if (validStr != null) {
                System.err.println(validStr);
                printUsage();
                System.exit(2);
            }
        } catch (Exception ex) {
            System.err.println("Unable to parse arguments. Error: " + ex.getMessage());
            printUsage();
            System.exit(2);
        }

        System.err.println("Profiling agent is starting with configuration:");
        System.err.print(config.getParamsValueStr());

        // Apply config
        Log.setLogConfig( config.logFn, config.logLevel );
        ProfilerEngine.init( config);

        final boolean need2printResults = config.resFn==null;

        // Create shout down hook first. Note, it doesn't survive kill signal
        // When app is finished, we need to print results.
        // Note: Results from history will be printed second time.
        //      No needs to stre then second time
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (need2printResults) {
                    ProfilerEngine.printResults();
                }
            }
        });

        Log.info("Agent is starting...");
        // Here we setup transformer for classes that we will load
        AgentFileTransformer transformer = new AgentFileTransformer(config);
        instrumentation.addTransformer(transformer);

        // Prepare objects that we need to track...
        // Set has a '/' separated class names.
        final ConcurrentHashMap<String,String> trackingObjectsSet = new ConcurrentHashMap<>();
        for (String obj2tr : config.object2track) {
            obj2tr = obj2tr.replace('.', '/');
            trackingObjectsSet.put(obj2tr,obj2tr);
        }

        // Now we need to instrument
        //transformer.instrumentLoadedClasses();

        AllocationRecorder.addSampler(new Sampler() {
            public void sampleAllocation(int count, String desc, Object newObj, long size) {
                /*System.out.println("I just allocated the object " + newObj
                        + " of type " + desc + " whose size is " + size);
                if (count != -1) { System.out.println("It's an array of size " + count); }*/
                // I just allocated the object  of type java/lang/StringBuilder whose size is 24

                //I just allocated the object [C@76e7a7b1 of type char whose size is 296
                //It's an array of size 140

                if (trackingObjectsSet.contains(desc))
                    ProfilerEngine.processTrackingObject();

                ProfilerEngine.processmemoryUsage(size);
            }
        });
    }

    private static void printUsage() {
        System.err.println("Usage: java -javaagent:<path>/profiler-1.0.jar=<arguments>");
        System.err.println("Please specify parameters with arguments string");
        System.err.print( AgentConfig.getParamsInfo() );
    }
}
