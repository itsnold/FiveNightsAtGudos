@echo off
setlocal
set "BASE_DIR=%~dp0."
set "JAVA_CMD=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
"%JAVA_CMD%" "-Dmaven.multiModuleProjectDirectory=%BASE_DIR%" -classpath "%BASE_DIR%\.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %errorlevel%
