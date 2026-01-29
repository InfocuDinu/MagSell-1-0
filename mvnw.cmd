@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@IF "%OS%"=="Windows_NT" @SETLOCAL

set ERROR_CODE=0

set MAVEN_PROJECTBASEDIR=%~dp0
@REM Remove trailing backslash
IF "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%

set WRAPPER_DIR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper
set WRAPPER_PROPERTIES=%WRAPPER_DIR%\maven-wrapper.properties
set WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar

if not exist "%WRAPPER_PROPERTIES%" (
  echo [ERROR] "%WRAPPER_PROPERTIES%" not found.
  set ERROR_CODE=1
  goto end
)

@REM Read wrapperUrl from properties (if present)
set WRAPPER_URL=
for /f "usebackq tokens=1,* delims==" %%A in ("%WRAPPER_PROPERTIES%") do (
  if "%%A"=="wrapperUrl" set WRAPPER_URL=%%B
)
if "%WRAPPER_URL%"=="" (
  set WRAPPER_URL=https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar
)

@REM Download wrapper jar if missing
if not exist "%WRAPPER_JAR%" (
  echo Downloading Maven Wrapper...
  if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%" >NUL 2>&1

  @REM Prefer curl.exe (more reliable than Invoke-WebRequest in some environments)
  curl.exe -fsSL "%WRAPPER_URL%" -o "%WRAPPER_JAR%"
  if errorlevel 1 (
    @REM Fallback to PowerShell
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$ProgressPreference='SilentlyContinue';" ^
      "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;" ^
      "Invoke-WebRequest -UseBasicParsing -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%';"
  )
  if errorlevel 1 (
    echo [ERROR] Failed to download Maven Wrapper jar from %WRAPPER_URL%
    set ERROR_CODE=1
    goto end
  )
)

@REM Find Java
if "%JAVA_HOME%"=="" (
  set JAVACMD=java
) else (
  set JAVACMD="%JAVA_HOME%\bin\java"
)

@REM Execute wrapper
%JAVACMD% -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*

:end
@ENDLOCAL & set ERROR_CODE=%ERROR_CODE%
exit /B %ERROR_CODE%

