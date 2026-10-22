@echo off
setlocal
pushd "%~dp0"
call mvnw.cmd -q -DskipTests package
if errorlevel 1 (
    popd
    exit /b 1
)
java --enable-native-access=ALL-UNNAMED -jar "target\five-nights-at-gudos.jar" %*
set "RESULT=%errorlevel%"
popd
exit /b %RESULT%
