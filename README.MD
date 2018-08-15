# WebGoat 8: A deliberately insecure Web Application

[![Build Status](https://https://travis-ci.com/kbay0/profiler.svg?branch=master)](https://travis-ci.com/kbay0/profiler)

# Introduction

Profiler is a Java based agent. The agent can be configured to profile any non core Java method in generic application. In case of Web Application, profiled information will be enriched with URL. Results of profiling can be viewable wuth URL that ends with 'profiler'.

For any profiled function agent calculate metrics:
- execution time
- Number of tracking objects created.
- Amount of allocated memory

This app was builded and tested with Java 8.

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
                             If this method exist, you will get results on a webpage with any url that ends with '/profile'
  logFile:<file name>        - Logs output to file insted of console
  logLevel:Err|Warn|Info|Debug|Trace - Logs level. Default is Err
  resultFile:<file name>     - Result output to file instead of console


Example below tracks usage of Strings and Long, in methods app2profile.Handlers.firstHandler and app2profile.Handlers.anotherHandler. Store results into the file 'profile.txt'

```
java -javaagent:lib/java-allocation-instrumenter-3.1.0.jar -javaagent:target/profiler-1.0.jar=obj2track:java.lang.String,obj2track:java.lang.Long,func2prof:app2profile.Handlers.firstHandler,func2prof:app2profile.Handlers.anotherHandler -cp <your_jars> <main_class>
```

