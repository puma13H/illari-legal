@echo off
setlocal EnableExtensions

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  call mvn %*
  exit /b %ERRORLEVEL%
)

set "MAVEN_VERSION=3.9.16"
set "MAVEN_DIR=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_CMD=%MAVEN_DIR%\bin\mvn.cmd"

if not exist "%MAVEN_CMD%" (
  echo [Illari] Maven no esta instalado. Descargando Apache Maven %MAVEN_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop';" ^
    "$v='%MAVEN_VERSION%';" ^
    "$url='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/'+$v+'/apache-maven-'+$v+'-bin.zip';" ^
    "$parent=Split-Path -Parent '%MAVEN_DIR%'; New-Item -ItemType Directory -Force -Path $parent | Out-Null;" ^
    "$zip=Join-Path $env:TEMP ('apache-maven-'+$v+'.zip');" ^
    "Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $zip;" ^
    "if(Test-Path '%MAVEN_DIR%'){Remove-Item -Recurse -Force '%MAVEN_DIR%'};" ^
    "Expand-Archive -Force -Path $zip -DestinationPath $parent;" ^
    "Remove-Item -Force $zip"
  if errorlevel 1 exit /b 1
)

call "%MAVEN_CMD%" %*
exit /b %ERRORLEVEL%
