#!/bin/bash

#Passing machine agent process id to java process.
../../jre/bin/java -cp "./*:../../machineagent.jar:/opt/mqm/java/lib/*" -Djava.library.path="/opt/mqm/java/lib64" -Dlog4j.configurationFile=./log4j2.xml -Dextension.configuration=./monitors/WMBMonitor/config.yml com.appdynamics.extensions.wmb.WMBMonitor $PPID

exit $?
