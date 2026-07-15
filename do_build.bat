@echo off
setlocal
REM Portable release build helper for OpenWrt-MGR
if not defined JAVA_HOME (
  if exist "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot" (
    set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
  )
)
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
if defined ANDROID_HOME set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
cd /d "%~dp0"
set TERM=dumb
call gradlew.bat :app:assembleRelease --no-daemon
if errorlevel 1 (
  echo BUILD FAILED
  exit /b 1
)
for %%F in (app\build\outputs\apk\release\*.apk) do (
  copy /Y "%%F" OpenWrt-MGR-release.apk >nul
  echo Copied %%F -^> OpenWrt-MGR-release.apk
)
echo DONE
endlocal
