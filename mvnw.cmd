@ECHO OFF
SETLOCAL

SET "BASE_DIR=%~dp0"
SET "WRAPPER_JAR=%BASE_DIR%\.mvn\wrapper\maven-wrapper.jar"

IF "%JAVA_HOME%"=="" (
  FOR %%I IN (java.exe) DO SET "JAVA_EXE=%%~$PATH:I"
) ELSE (
  SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)

IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO Missing Maven wrapper jar: %WRAPPER_JAR% 1>&2
  EXIT /B 1
)

IF "%JAVA_EXE%"=="" (
  ECHO Java runtime not found. Install JDK 25 or set JAVA_HOME. 1>&2
  EXIT /B 1
)

"%JAVA_EXE%" -Dmaven.multiModuleProjectDirectory="%BASE_DIR%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
