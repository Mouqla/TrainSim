@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

where javac >nul 2>&1
if errorlevel 1 (
  echo Java JDK 17 or newer is required. Install Java and try again.
  pause
  exit /b 1
)

if not exist bin mkdir bin
set "SOURCE_LIST=%TEMP%\trainsim-sources-%RANDOM%.txt"

> "%SOURCE_LIST%" (
  for /r "%CD%\src" %%F in (*.java) do (
    set "SOURCE_FILE=%%F"
    echo "!SOURCE_FILE:\=/!"
  )
)

javac -d bin @"%SOURCE_LIST%"
if errorlevel 1 goto compile_error

del "%SOURCE_LIST%" >nul 2>&1
if not exist bin\tsim\ui\bitmaps xcopy /E /I /Y src\TSim\ui\bitmaps bin\tsim\ui\bitmaps >nul

if /I "%~1"=="--compile-only" exit /b 0
java -cp bin JavaMain
exit /b %errorlevel%

:compile_error
del "%SOURCE_LIST%" >nul 2>&1
echo The project could not be compiled.
pause
exit /b 1
