package test.kbay;

import javassist.*;
import test.kbay.util.Log;
import test.kbay.util.ProfException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;

/**
 *  Profiler File transformer. During class loading process, we need to add some instrumentation:
 *  - Intercept Object creation by instrumenting Object ctor
 *  - Instrument caller functions that needed to profile.
 *  - Optionally for Spring instrument Dispatch method to generate result data on the page.
 *
 *  Profiler recognize functions by names only, there is no filtering by argument list.
 */
public class AgentFileTransformer implements ClassFileTransformer {

    // key: class, value: methods
    // class has path notation. Example: java/lang/ClassValue
//    private final HashMap< String, Set<String> > object2track    = new HashMap<>();
//    private final HashMap< String, Set<String> > mem2track    = new HashMap<>();
    private final HashMap< String, Set<String> > func2profile    = new HashMap<>();
    private final HashMap< String, Set<String>> dispatcher      = new HashMap<>();

    // classes that needed to be instrumented. path notation notation Example: java/lang/ClassValue
    private final HashSet<String>  need2instrumentClasses = new HashSet<String>();
    // Already instrumented classes. path notation notation Example: java/lang/ClassValue
    private final HashSet<String>  instrumentedClasses = new HashSet<String>();

    /**
     * AgentFileTransformer ctor. Accept function names in format:  <package>.<class>::<Function name>
     *
     * @param config - config file that has all info about instrumented functions
     * @throws ProfException
     */
    public AgentFileTransformer( AgentConfig config ) {
        for ( String func : config.funcNames2profile ) {
            int idx = func.lastIndexOf(".");
            if (idx<=0) {
                Log.error("Ignoring wrong method name to profile: " + func);
                continue;
            }
            addMetod4Class( func2profile, func.substring(0,idx), func.substring(idx+1) );
        }

        for ( String func : config.functNameDispatcher ) {
            int idx = func.lastIndexOf(".");
            if (idx<=0) {
                Log.error("Ignoring wrong method name to profile: " + func);
                continue;
            }
            addMetod4Class( dispatcher, func.substring(0,idx), func.substring(idx+1) );
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        // check if class need to be instrumented
        if ( !need2instrumentClasses.contains(className) ) {
            Log.trace("Profiler skipping a class " + className + ", not needed to instrument");
            return classfileBuffer;
        }

        Log.debug("Transform expects to instrument a class " + className);

        byte[] bytecode = classfileBuffer;
        try {
            ClassPool cPool = ClassPool.getDefault();
            CtClass ctClass = cPool.makeClass(new ByteArrayInputStream(bytecode));

            instrumentClass( className, ctClass) ;

            bytecode = ctClass.toBytecode();
            Log.debug("Transform finished for: " + className);

        } catch (IOException e) {
            throw new IllegalClassFormatException(e.getMessage());
        } catch (RuntimeException e) {
            throw new IllegalClassFormatException(e.getMessage());
        } catch (CannotCompileException e) {
            throw new IllegalClassFormatException(e.getMessage());
        }
        return bytecode;
    }

    //////////////////////////////////////////////////////////////////

    // className has path notation. Example: java/lang/ClassValue
    private synchronized void instrumentClass( String className, CtClass ctClass) throws CannotCompileException {

        if (instrumentedClasses.contains(className))
            return;

        instrumentedClasses.add(className);

        Log.info("Instrumenting class: " + className );//.replace('/','.') );

        Set<String> methods2profile = func2profile.get(className);
        Set<String> methods4dispatcher = dispatcher.get(className);

        // Instrumrnting methods for profiling
        if (methods2profile != null) {
            for (String method : methods2profile) {
                try {
                    CtMethod[] methods = ctClass.getDeclaredMethods(method);
                    for ( CtMethod mt : methods) {

                        Log.info("Instrumenting method for profiling: " +  mt.getLongName() );

                        CtClass[] pTypes = mt.getParameterTypes();
                        int servReqIdx = -1;
                        for( int i=0; i < pTypes.length; ++i ) {
                            CtClass type = pTypes[i];
                            String typeName = type.getName();
                            Log.debug(mt.getLongName() + " argument " + i + " type: " +  typeName );
                            if ( "javax.servlet.http.HttpServletRequest".equals(typeName) ) {
                                servReqIdx = i;
                                break;
                            }
                        }

                        if (servReqIdx<0)
                            mt.insertBefore( "{ test.kbay.profiler.ProfilerEngine.startProfiling( \"" + mt.getLongName() + "\", null); }" );
                        else
                            mt.insertBefore( "{ test.kbay.profiler.ProfilerEngine.startProfiling( \"" + mt.getLongName() + "\", $args["+servReqIdx+"]); }" );

                        mt.insertAfter("{test.kbay.profiler.ProfilerEngine.finishProfiling();}", true); // as final
                    }
                }
                catch (Exception ex) {
                    Log.error("Failed to instrument method " + className.replace('/','.') + "." + method, ex);
                }
            }
        }

        // Instrumrnting dispatch method. It has to return void and accept two arguments HttpServletRequest & HttpServletResponse
        if (methods4dispatcher != null) {
            for (String method : methods4dispatcher) {
                try {
                    CtMethod[] methods = ctClass.getDeclaredMethods(method);
                    for ( CtMethod mt : methods) {

                        Log.info("Instrumenting dispatch method: " +  mt.getLongName() );

                        // Expected void return type for dispatcher
                        if (! CtClass.voidType.getName().equals(mt.getReturnType().getName() ) ) {
                            Log.error("Profiler unable to instrument dispatcher because expected return type not found for method " + mt.getLongName() );
                            continue;
                        }


                        CtClass[] pTypes = mt.getParameterTypes();
                        int servReqIdx = -1;
                        int servRespIdx = -1;
                        for( int i=0; i < pTypes.length; ++i ) {
                            CtClass type = pTypes[i];
                            String typeName = type.getName();
                            Log.debug(mt.getLongName() + " argument " + i + " type: " +  typeName );
                            if ( "javax.servlet.http.HttpServletRequest".equals(typeName) ) {
                                servReqIdx = i;
                            }

                            if ( "javax.servlet.http.HttpServletResponse".equals(typeName) ) {
                                servRespIdx = i;
                            }
                        }

                        if ( servReqIdx<0 || servRespIdx<0 ) {
                            Log.error("Profiler unable to instrument dispatcher because expected arguments are not found for method " + mt.getLongName() );
                        }
                        else {
                            // Method must return void.

                           /* {
                                javax.servlet.http.HttpServletRequest request = (javax.servlet.http.HttpServletRequest)$args[servReqIdx];
                                javax.servlet.http.HttpServletResponse response = (javax.servlet.http.HttpServletResponse)$args[servRespIdx];
                                if (request!=null && response!=null) {
                                    StringBuffer url = request.getRequestURL();
                                    if (url!=null && url.toString().endsWith("profiler")) {
                                        response.setStatus(200);
                                        java.io.PrintWriter writer = response.getWriter();

                                        if (test.kbay.profiler.ProfilerEngine.onPrintProfilingResults( writer ))
                                            return;
                                    }
                                }
                            }*/

                            // have to extract all Data inplace due Java security
                            mt.insertBefore(
                             "{\n" +
                                     "javax.servlet.http.HttpServletRequest request = (javax.servlet.http.HttpServletRequest)$args["+servReqIdx+"];\n" +
                                     "javax.servlet.http.HttpServletResponse response = (javax.servlet.http.HttpServletResponse)$args["+servRespIdx+"];\n" +
                                     "if (request!=null && response!=null) {\n" +
                                     "                                    StringBuffer url = request.getRequestURL();\n" +
                                     "                                    if (url!=null && url.toString().endsWith(\"profiler\")) {\n" +
                                     "                                        response.setStatus(200);\n" +
                                     "                                        java.io.PrintWriter writer = response.getWriter();\n" +
                                     "\n" +
                                     "                                        if (test.kbay.profiler.ProfilerEngine.onPrintProfilingResults( writer ))\n" +
                                     "                                            return;\n" +
                                     "                                    }\n" +
                                     "                                }}"
                            );


                            //mt.insertBefore( "{ if (test.kbay.profiler.ProfilerEngine.onDispatcherRequest(  $args["+servReqIdx+"],  $args["+servRespIdx+"] )) return; }" );
                        }

                    }
                }
                catch (Exception ex) {
                    Log.error("Failed to instrument method " + className.replace('/','.') + "." + method, ex);
                }
            }
        }
    }



    private void addMetod4Class(HashMap< String, Set<String> > track, String className, String methodName ) {
        className = className.replace('.', '/'); // Example of calss name from insrument: java/io/FileOutputStream
        need2instrumentClasses.add(className);
        Set<String> methSet = track.get(className);
        if (methSet==null) {
            methSet = new HashSet<>();
            track.put(className, methSet);
        }
        methSet.add(methodName);
    }



}


