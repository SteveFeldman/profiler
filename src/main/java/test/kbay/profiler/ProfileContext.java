package test.kbay.profiler;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ProfileContext represents single function call that we need to profile.
 */
class ProfileContext {
    final String contextName; // name for further reference

    final int    threadHash; // hash code for thread context was created for.

    final long   startTime;
    long         finishTime;

    // Counters for tracking Classes and Total Memory consumed by objects
    private final AtomicInteger thrObjs = new AtomicInteger(0);
    private final AtomicLong    thrMem      = new AtomicLong(0);
    private final AtomicInteger allObjs = new AtomicInteger(0);
    private final AtomicLong    allMem     = new AtomicLong(0);

    /**
     * Create context for current thread
     */
    ProfileContext(String contextName) {
        this.contextName = contextName;
        this.threadHash = Thread.currentThread().hashCode();
        startTime = System.currentTimeMillis();
        finishTime = startTime;
    }

    /**
     * Track object
     * @param thrHash - Thread hash
     */
    public void onObjectTracking( int thrHash ) {
        if ( thrHash == threadHash ) {
            thrObjs.incrementAndGet();
            //if (!Log.checkIfLog( Log.LEVEL_DEBUG ) )
            //    new Exception().printStackTrace(System.out);
        }

        allObjs.incrementAndGet();
    }

    /**
     * Track memory
     * @param thrHash - Thread hash
     * @param mem  - memory used by the item
     */
    public void onMemoryTracking( int thrHash, long mem ) {
        if ( thrHash == threadHash )
            thrMem.addAndGet(mem);

        allMem.addAndGet(mem);
    }

    /**
     * Finish tracking
     */
    public void onFinishProcessing() {
        finishTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "ProfileContext: contextName=" + contextName + " threadHash=" + threadHash +
                " Thread=" + thrObjs.get() + "/" + thrMem.get() + " All=" + allObjs.get() + "/" + allMem.get();
    }

    /**
     * Result as a string for user's output
     * @param shortStr - Use short string or full user friendly
     * @return profiling result
     */
    public String getResultStr(boolean shortStr) {

        if (shortStr)
            return contextName + ";" + thrObjs.get() + ";" + thrMem.get() + ";" + allObjs.get() + ";" + allMem.get();

        return contextName + " Execution time:" + (finishTime - startTime) + " ms. In method's thread created tracking objects: " + thrObjs.get() + ", consumed memory: " + thrMem.get() + " bytes. " +
                "In all threads created tracking objects: " + allObjs.get() + ", consumed memory: " + allMem.get() + " bytes.";
    }

}
