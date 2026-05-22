@rem Gradle wrapper script for Windows
@if "%DEBUG%"=="" @echo off
setlocal
set DIRNAME=%~dp0
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if not exist "%CLASSPATH%" (
    echo Downloading gradle-wrapper.jar...
    mkdir "%APP_HOME%\gradle\wrapper" 2>nul
    powershell -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.5-bin.zip' -OutFile '%TEMP%\gradle.zip'"
    powershell -Command "Expand-Archive -Path '%TEMP%\gradle.zip' -DestinationPath '%TEMP%\gradle-temp' -Force"
    copy "%TEMP%\gradle-temp\gradle-8.5\lib\gradle-wrapper-8.5.jar" "%CLASSPATH%" >nul 2>&1
    rmdir /s /q "%TEMP%\gradle-temp" 2>nul
    del "%TEMP%\gradle.zip" 2>nul
)

java %JAVA_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
