#!/bin/bash

#Passing machine agent process id to java process.
../../jre/bin/java -cp "./*:../../machineagent.jar:lib/*" -Dlog4j.configuration=./log4j.xml -Dextension.configuration=./monitors/WMBMonitor/config.yml com.appdynamics.extensions.wmb.WMBMonitor $PPID

exit $?
