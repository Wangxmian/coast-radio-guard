@ECHO OFF
SETLOCAL
SET BASE_DIR=%~dp0
SET WRAPPER_DIR=%BASE_DIR%\.mvn\wrapper
SET WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar
SET WRAPPER_PROPERTIES=%WRAPPER_DIR%\maven-wrapper.properties

IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO maven-wrapper.jar not found, downloading...
  FOR /F "tokens=1,* delims==" %%A IN (%WRAPPER_PROPERTIES%) DO (
    IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
  )
  powershell -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
)

"%JAVA_HOME%\bin\java" -Dmaven.multiModuleProjectDirectory="%BASE_DIR%" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
ENDLOCAL
