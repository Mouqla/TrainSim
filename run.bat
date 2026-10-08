@echo off
setlocal

where javac >nul 2>&1
if errorlevel 1 (
  echo Java JDK 17 or newer is required. Install Java and try again.
  pause
  exit /b 1
)

if not exist bin mkdir bin
javac -sourcepath src -d bin src\Main.java
if errorlevel 1 (
  echo The project could not be compiled.
  pause
  exit /b 1
)

java -cp bin JavaMain
