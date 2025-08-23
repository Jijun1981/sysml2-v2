@echo off
setlocal enabledelayedexpansion

echo ============================================
echo   SysML v2 MVP Development Environment
echo ============================================

:: 设置项目根目录
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..

:: 检查Java环境
echo Checking Java environment...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Please install Java 17 or higher.
    exit /b 1
)

:: 检查Maven环境
echo Checking Maven environment...
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Maven not found. Please install Maven.
    exit /b 1
)

:: 检查Node.js环境
echo Checking Node.js environment...
node -v >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js not found. Please install Node.js 16 or higher.
    exit /b 1
)

:: 创建数据目录
echo Creating data directories...
if not exist "%PROJECT_ROOT%\data" mkdir "%PROJECT_ROOT%\data"
if not exist "%PROJECT_ROOT%\data\projects" mkdir "%PROJECT_ROOT%\data\projects"
if not exist "%PROJECT_ROOT%\data\backups" mkdir "%PROJECT_ROOT%\data\backups"
if not exist "%PROJECT_ROOT%\data\demo" mkdir "%PROJECT_ROOT%\data\demo"
if not exist "%PROJECT_ROOT%\data\logs" mkdir "%PROJECT_ROOT%\data\logs"

:: 检查参数
if "%1"=="--install" (
    echo.
    echo Installing backend dependencies...
    cd /d "%PROJECT_ROOT%\backend"
    call mvn clean install -DskipTests
    
    echo.
    echo Installing frontend dependencies...
    cd /d "%PROJECT_ROOT%\frontend"
    call npm install
)

:: 启动后端服务
echo.
echo Starting backend service...
cd /d "%PROJECT_ROOT%\backend"
start "SysML Backend" cmd /c "mvn spring-boot:run"

:: 等待后端启动
echo Waiting for backend to start...
timeout /t 10 /nobreak >nul

:: 启动前端服务
echo.
echo Starting frontend service...
cd /d "%PROJECT_ROOT%\frontend"
start "SysML Frontend" cmd /c "npm run dev"

echo.
echo ============================================
echo   Development environment started!
echo ============================================
echo Backend: http://localhost:8080/api/v1
echo Frontend: http://localhost:3000
echo API Docs: http://localhost:8080/api/v1/swagger-ui.html
echo Health: http://localhost:8080/api/v1/health
echo.
echo Close this window to stop all services
echo ============================================

pause