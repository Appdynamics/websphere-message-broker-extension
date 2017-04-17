@echo off

java -cp "..\..\machineagent.jar;lib\*" -Dlog4j.configuration=file:.\log4j.xml -Dextension.configuration=file:.\config.yml -jar websphere-message-broker-extension.jar

