@echo off
REM Payanam Rail - ready-to-run build. Needs only Java 17 or newer.
cd /d "%~dp0"
echo Starting Payanam Rail on http://localhost:8080 ...
start "" cmd /c "timeout /t 15 >nul && start http://localhost:8080"
java -jar payanam-rail-1.0.0.jar
pause
