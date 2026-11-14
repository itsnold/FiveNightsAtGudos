@echo off
setlocal
rem Use one JDK 17+ for both Maven and the game; do not change the system Java settings.
set "GAME_JAVA_HOME="
call :tryJdk "%JAVA_HOME%"
for /f "delims=" %%J in ('where javac 2^>nul') do call :tryJdk "%%~dpJ.."
call :tryJdk "%ProgramFiles%\Apache NetBeans\jdk"
call :tryJdk "%ProgramFiles%\Android\Android Studio\jbr"
for /d %%J in ("%ProgramFiles%\Java\jdk*" "%ProgramFiles%\Eclipse Adoptium\jdk*" "%ProgramFiles%\Microsoft\jdk*") do call :tryJdk "%%~fJ"
if not defined GAME_JAVA_HOME (
    echo A JDK 17 or newer is required. Install one and set JAVA_HOME to its folder.
    exit /b 1
)
set "JAVA_HOME=%GAME_JAVA_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using Java from "%JAVA_HOME%"
pushd "%~dp0"
call mvnw.cmd -q -DskipTests package
if errorlevel 1 (
    popd
    exit /b 1
)
"%JAVA_HOME%\bin\java.exe" --enable-native-access=ALL-UNNAMED -jar "target\five-nights-at-gudos.jar" %*
set "RESULT=%errorlevel%"
popd
exit /b %RESULT%

:tryJdk
if defined GAME_JAVA_HOME exit /b
if "%~1"=="" exit /b
if not exist "%~1\bin\java.exe" exit /b
if not exist "%~1\bin\javac.exe" exit /b
set "JDK_VERSION="
pushd "%~1\bin"
for /f "tokens=2" %%V in ('.\javac.exe -version 2^>^&1 ^| findstr /b /c:"javac "') do set "JDK_VERSION=%%V"
popd
if not defined JDK_VERSION exit /b
for /f "tokens=1 delims=.-" %%V in ("%JDK_VERSION%") do if %%V GEQ 17 set "GAME_JAVA_HOME=%~f1"
exit /b
