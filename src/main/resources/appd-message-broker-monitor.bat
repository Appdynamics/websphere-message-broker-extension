@echo off

java -cp "./*;..\..\machineagent.jar;lib\*" -Dlog4j.configuration=.\log4j.xml -Dextension.configuration=.\monitors\WMBMonitor\config.yml com.appdynamics.extensions.wmb.WMBMonitor

