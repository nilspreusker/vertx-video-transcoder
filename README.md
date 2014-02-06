# Vert.x Video Transcoder

This is an example project, showcasing how FFmpeg can be wrapped as a vert.x module.

## Before you start
* make sure you have an FFmpeg executable
* copy the file example.config.json and modify the properties to match your setup
* you will need a Java SDK 7 or later and Apache Maven 3.x or later to run the example

## Running the example
* check out the example, open a terminal and cd to the directory where the code is located
(e.g. cd ~/code/vertx-video-transcoder)
* run 'mvn clean package' to build the module
* run 'mvn vertx:runMod' to run the module

## Debugging the example
The easiest way to debug the example is to import it into your favorite IDE and create a Maven run configuration which
you are able to start in debug mode. In IntelliJ IDEA 13, this requires the following steps:
Go to "Run > Edit Configurations...", click the '+' in the upper left corner and select "Maven". Provide a name for
the run configuration e.g. "vert.x run module". If the working directory is not already set, set it to the directory
of your project. In the field "Command line" fill in "vertx:runMod" and save the run configuration. Now, if you set
a breakpoint in the start method of the verticle and select "Run > Debug 'vert.x run module'", the debugger should
stop at your breakpoint.


Now go and have fun!
