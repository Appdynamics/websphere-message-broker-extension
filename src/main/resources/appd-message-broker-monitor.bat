@echo off

﻿setlocal enabledelayedexpansion

REM ****Get the MA process id****
SET /a lineCount=1
FOR /f "usebackq skip=1" %%i in (`wmic process where ^"commandline like ^'%%appd-message-broker-monitor.bat%%^'^" get processid`) DO (
	IF !lineCount! == 1 (
		SET CURR_PROCESS_ID=%%i
	)
	SET /a lineCount += 1
)


REM ****Get Parent process's i.e. MA process id****
SET /a lineCount=1
FOR /f "tokens=1" %%a in ('wmic process where ^(processid^=!CURR_PROCESS_ID!^) get parentprocessid') DO (
	IF !lineCount! == 2 (
		SET PARENT_PROCESS_ID=%%a
	)
	SET /a lineCount += 1
)

REM ****Pass MA process id to the java process****
..\..\jre\bin\java -cp ".\*;..\..\machineagent.jar;C:\Program Files\IBM\MQ\java\lib\*" -Djava.library.path=C:\Program Files\IBM\MQ\java\lib64 -Dlog4j.configuration=file:.\log4j.xml -Dextension.configuration=.\monitors\WMBMonitor\config.yml com.appdynamics.extensions.wmb.WMBMonitor !PARENT_PROCESS_ID!

endlocal
