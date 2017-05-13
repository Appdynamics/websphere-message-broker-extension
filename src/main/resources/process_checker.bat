@echo off

setlocal enabledelayedexpansion
REM ***Check if MA process is running***
SET /a lineCount=1
FOR /f "skip=1" %%j in ('wmic process where ^(processid^=%1^) get processid') DO (
    IF !lineCount! == 1 (
    	echo.%%j
    )
    SET /a lineCount += 1
)

endlocal