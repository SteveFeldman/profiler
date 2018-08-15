# Profiler: Simple Java Agent for prifiling metod calls.

[![Build Status](https://travis-ci.com/kbay0/profiler.svg?branch=master)](https://travis-ci.com/kbay0/profiler)

# Introduction

This profiler is not intended to use in production. It is just a demo application.
The agent can be configured to profile any non core Java method in generic application. In case of Web Application, profiled information will be enriched with URL. Results of profiling can be viewable with URL that ends with 'profiler'.

For any profiled function agent calculate metrics:
- execution time
- Number of tracking objects created (in method's thread, all threads)
- Amount of allocated memory (in method's thread, all threads)

This app was builded and tested with Java 8.

Please note, the sources include jars that are used for intergation testing and some third party libraries.

# Run Instructions:

## Run from the sources

### Prerequisites:

* Java 8
* Maven > 3.2.5
* Git

Open a command shell/window:

```Shell
git clone https://github.com/kbay0/profiler.git
```

Now let's start by compiling the project.

```Shell
cd profiler
mvn clean install
```

Now we are ready to run the profiler.

Check the usage:
```Shell
java -javaagent:lib/java-allocation-instrumenter-3.1.0.jar -javaagent:target/profiler-1.0.jar -cp <your_jars> <main_class>
```
You should see the output:
```
Usage: java -javaagent:<path>/profiler-1.0.jar=<arguments>
Please specify parameters with arguments string
All parameters are comma separated (','). objCtor and func2prof can be included multiple times
Example: objCtor=java.lang.String;objCtor=java.lang.Number;func2prof=my.test.method
  hist:50                    - how many calls tp profiles funtions profiler keeps in the history
  obj2track:java.lang.String - object usage that need to be profiled
  func2prof:my.test.method   - method that we need to profile. If method
                             has argument javax.servlet.http.HttpServletRequest, profile call will be
                             updated with url
  dispatch:org.springframework.web.servlet.DispatcherServlet.doDispatch - Dispatch method for your webserver.
                             If this method exist, you will get results on a webpage with any url that ends with '/profiler'
  logFile:<file name>        - Logs output to file insted of console
  logLevel:Err|Warn|Info|Debug|Trace - Logs level. Default is Err
  resultFile:<file name>     - Result output to file instead of console
```


Example below tracks usage of Strings and Long, in methods app2profile.Handlers.firstHandler and app2profile.Handlers.anotherHandler. Store results into the file 'profile.txt'

```
java -javaagent:lib/java-allocation-instrumenter-3.1.0.jar -javaagent:target/profiler-1.0.jar=obj2track:java.lang.String,obj2track:java.lang.Long,func2prof:app2profile.Handlers.firstHandler,func2prof:app2profile.Handlers.anotherHandler,resultFile=profile.txt -cp <your_jars> <main_class>
```
Output will be:
```
Profiling agent is starting with configuration:
hist:50
obj2track 2 items:
  java.lang.Long
  java.lang.String
func2prof 2 items:
  app2profile.Handlers.firstHandler
  app2profile.Handlers.anotherHandler
dispatch 1 items
  org.springframework.web.servlet.DispatcherServlet.doDispatch
logFile:CONSOLE
logLevel:Err
resultFile:profile.txt
...........
..............
2018-08-14 20:45:28 app2profile.Handlers.firstHandler() Execution time:1012 ms. In method's thread created tracking objects: 3, consumed memory: 1208 bytes. In all threads created tracking objects: 12, consumed memory: 3440 bytes.
..............
................
Profiler result for last 1 items:
2018-08-14 20:45:28 app2profile.Handlers.firstHandler() Execution time:1012 ms. In method's thread created tracking objects: 3, consumed memory: 1208 bytes. In all threads created tracking objects: 12, consumed memory: 3440 bytes.
```
# Run with WebGoat and verify the workflow

Web goat is located at test_lib/webgoat-server-8.0.0.M21.jar

1. Cd to your project root dir
```
cd <dev_root>
```
2. Start webgoat, profiler will use debug output.
```
java -javaagent:lib/java-allocation-instrumenter-3.1.0.jar  -javaagent:target/profiler-1.0.jar=func2prof:org.springframework.web.servlet.DispatcherServlet.doDispatch,obj2track:java.lang.String,logLevel:Debug -jar test_lib/webgoat-server-8.0.0.M21.jar
```
In the output you should see:
Info  | Instrumenting class: org/springframework/web/servlet/DispatcherServlet
Debug | Transform finished for: org/springframework/web/servlet/DispatcherServlet

3. Wait until webserver will be ready (could take up to 60 secondes) and print 'Tomcat started on port(s): 8080 (http)'

4. Open you browser on the same host and type 'http://localhost:8080/WebGoat/login'
In the output you shoudl see that profiler did some action.

5. Check profiler results by typing url 'http://localhost:8080/WebGoat/css/img/profiler'