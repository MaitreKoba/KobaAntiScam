@echo off
set JAVA_HOME=D:\Kobayashi\Developpement\Java\jdk-17\jdk-17.0.12+7
set PATH=%JAVA_HOME%\bin;%PATH%
call gradlew.bat %* > build_output.txt 2>&1
echo Build complete, see build_output.txt
pause
