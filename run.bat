@echo off
setlocal enabledelayedexpansion
title Payanam Rail Launcher

echo =======================================================================
echo                     PAYANAM RAIL LAUNCHER
echo =======================================================================
echo.

REM -----------------------------------------------------------------------
REM 1. Paths and Ports Configuration (from application.yml & vite.config.ts)
REM -----------------------------------------------------------------------
set "ROOT_DIR=%~dp0"
set "BACKEND_DIR=%~dp0backend"
set "FRONTEND_DIR=%~dp0frontend"

set "BACKEND_PORT=8080"
set "FRONTEND_PORT=5173"
set "FRONTEND_URL=http://localhost:5173"
set "BACKEND_URL=http://localhost:8080"

REM -----------------------------------------------------------------------
REM 2. Dependency Checks (uses GOTO to prevent parenthesized block parser errors)
REM -----------------------------------------------------------------------
echo [Step 1/3] Checking prerequisites and dependencies...

REM Check Java (JDK 17+)
where java >nul 2>&1
if errorlevel 1 goto ERR_JAVA

REM Check Node.js
where node >nul 2>&1
if errorlevel 1 goto ERR_NODE

REM Check npm
where npm >nul 2>&1
if errorlevel 1 goto ERR_NPM

REM Check Backend directory and Maven wrapper
if not exist "%BACKEND_DIR%\mvnw.cmd" goto ERR_MVNW
if not exist "%BACKEND_DIR%\pom.xml" goto ERR_POM

REM Check Frontend directory and package.json
if not exist "%FRONTEND_DIR%\package.json" goto ERR_PACKAGE_JSON

REM Check Frontend dependencies (node_modules)
if not exist "%FRONTEND_DIR%\node_modules" goto ERR_NODE_MODULES

echo   [OK] Java is installed.
echo   [OK] Node.js and npm are installed.
echo   [OK] Backend Maven wrapper and configuration verified.
echo   [OK] Frontend package and node_modules verified.
echo.

REM -----------------------------------------------------------------------
REM 3. Check for Existing Running Processes (Prevent Duplicates)
REM -----------------------------------------------------------------------
echo [Step 2/3] Checking for active instances...

set "BACKEND_RUNNING=0"
set "FRONTEND_RUNNING=0"

netstat -ano -p tcp | findstr /R /C:":%BACKEND_PORT% " | findstr "LISTENING" >nul 2>&1
if not errorlevel 1 set "BACKEND_RUNNING=1"

netstat -ano -p tcp | findstr /R /C:":%FRONTEND_PORT% " | findstr "LISTENING" >nul 2>&1
if not errorlevel 1 set "FRONTEND_RUNNING=1"

if "%BACKEND_RUNNING%"=="1" if "%FRONTEND_RUNNING%"=="1" goto BOTH_RUNNING

REM -----------------------------------------------------------------------
REM 4. Start Servers in Separate Terminal Windows
REM -----------------------------------------------------------------------
echo [Step 3/3] Starting servers...

if "%BACKEND_RUNNING%"=="1" (
    echo   - Spring Boot backend is already listening on port %BACKEND_PORT%. Skipping backend launch.
) else (
    echo   - Starting Spring Boot backend in a separate window on port %BACKEND_PORT%...
    start "Payanam Rail - Backend (Port %BACKEND_PORT%)" /D "%BACKEND_DIR%" cmd /k "mvnw.cmd spring-boot:run -Dskip.frontend=true"
)

if "%FRONTEND_RUNNING%"=="1" (
    echo   - React frontend is already listening on port %FRONTEND_PORT%. Skipping frontend launch.
) else (
    echo   - Starting React frontend in a separate window on port %FRONTEND_PORT%...
    start "Payanam Rail - Frontend (Port %FRONTEND_PORT%)" /D "%FRONTEND_DIR%" cmd /k "npm run dev"
)

REM -----------------------------------------------------------------------
REM 5. Wait for Frontend Server to Become Ready and Open Browser
REM -----------------------------------------------------------------------
if "%FRONTEND_RUNNING%"=="1" goto OPEN_BROWSER

echo.
echo Waiting for frontend server to become ready on %FRONTEND_URL%...
set /a ATTEMPTS=0

:WAIT_LOOP
ping -n 2 127.0.0.1 >nul
set /a ATTEMPTS+=1

netstat -ano -p tcp | findstr /R /C:":%FRONTEND_PORT% " | findstr "LISTENING" >nul 2>&1
if not errorlevel 1 goto FRONTEND_READY

if !ATTEMPTS! GEQ 45 (
    echo [WARNING] Frontend server is taking longer than expected to start.
    goto OPEN_BROWSER
)
goto WAIT_LOOP

:FRONTEND_READY
ping -n 2 127.0.0.1 >nul
goto OPEN_BROWSER

:OPEN_BROWSER
echo.
echo =======================================================================
echo  Payanam Rail is ready - opening in your default browser...
echo =======================================================================
start "" "%FRONTEND_URL%"

echo.
echo  - Frontend Website : %FRONTEND_URL%
echo  - Backend REST API : %BACKEND_URL%
echo  - Swagger UI Docs  : %BACKEND_URL%/swagger-ui.html
echo.
echo  Demo Sign-In Accounts:
echo   * Passenger:           passenger@payanam.in      / Passenger@123
echo   * Station Master:      stationmaster@payanam.in  / Station@123
echo   * Platform Controller: controller@payanam.in     / Station@123
echo   * Booking Clerk:       clerk@payanam.in          / Station@123
echo.
echo  Backend and frontend logs are running in separate terminal windows.
echo  To stop the servers, close their respective terminal windows or press Ctrl+C.
echo =======================================================================
ping -n 7 127.0.0.1 >nul
exit /b 0

:BOTH_RUNNING
echo.
echo [INFO] Both Spring Boot backend (port %BACKEND_PORT%) and React frontend (port %FRONTEND_PORT%) are already running.
echo [INFO] Opening %FRONTEND_URL% in your default browser...
start "" "%FRONTEND_URL%"
echo.
echo Done. Duplicate launch prevented.
ping -n 6 127.0.0.1 >nul
exit /b 0

REM -----------------------------------------------------------------------
REM Error Handlers
REM -----------------------------------------------------------------------
:ERR_JAVA
echo.
echo =======================================================================
echo [ERROR] Java is not installed or not found in system PATH.
echo Please install JDK 17 or newer (e.g. Eclipse Adoptium Temurin or OpenJDK).
echo Download: https://adoptium.net/
echo =======================================================================
echo.
pause
exit /b 1

:ERR_NODE
echo.
echo =======================================================================
echo [ERROR] Node.js is not installed or not found in system PATH.
echo Please install Node.js 20+ from https://nodejs.org/
echo =======================================================================
echo.
pause
exit /b 1
:ERR_NPM
echo.
echo =======================================================================
echo [ERROR] npm is not installed or not found in system PATH.
echo Please ensure npm is installed and available in system PATH.
echo =======================================================================
echo.
pause
exit /b 1
:ERR_MVNW
echo.
echo =======================================================================
echo [ERROR] Backend folder or Maven wrapper is missing.
echo Expected file: "%BACKEND_DIR%\mvnw.cmd"
echo =======================================================================
echo.
pause
exit /b 1
:ERR_POM
echo.
echo =======================================================================
echo [ERROR] Backend pom.xml is missing.
echo Expected file: "%BACKEND_DIR%\pom.xml"
echo =======================================================================
echo.
pause
exit /b 1
:ERR_PACKAGE_JSON
echo.
echo =======================================================================
echo [ERROR] Frontend folder or package.json is missing.
echo Expected file: "%FRONTEND_DIR%\package.json"
echo =======================================================================
echo.
pause
exit /b 1
:ERR_NODE_MODULES
echo.
echo =======================================================================
echo [ERROR] Frontend dependencies are missing.
echo Directory not found: "%FRONTEND_DIR%\node_modules"
echo.
echo Please install frontend dependencies before running:
echo   cd frontend
echo   npm install
echo =======================================================================
echo.
pause
exit /b 1