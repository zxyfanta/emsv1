@echo off
echo ========================================
echo EMS Docker测试环境启动脚本
echo ========================================
echo.

echo 正在检查Docker环境...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: Docker未安装或未运行
    echo 请先安装Docker Desktop并启动Docker服务
    pause
    exit /b 1
)

echo ✅ Docker环境检查通过
echo.

echo 正在检查Docker Compose...
docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: Docker Compose未安装
    pause
    exit /b 1
)

echo ✅ Docker Compose检查通过
echo.

echo 正在清理旧容器和数据...
docker-compose down -v >nul 2>&1

echo 正在构建并启动EMS测试环境...
docker-compose up -d --build

if %errorlevel% neq 0 (
    echo ❌ 错误: Docker Compose启动失败
    pause
    exit /b 1
)

echo.
echo ✅ Docker容器启动成功！
echo.
echo 正在等待服务启动完成...
timeout /t 30 /nobreak >nul

echo.
echo 正在检查服务状态...
docker-compose ps

echo.
echo ========================================
echo 🌐 服务访问地址:
echo ========================================
echo EMS后端API:         http://localhost:8081/api
echo H2数据库控制台:      http://localhost:8081/api/h2-console
echo Node-RED编辑器:      http://localhost:1880
echo MQTT Broker:        localhost:1883
echo ========================================

echo.
echo 正在检查服务健康状态...
timeout /t 30 /nobreak >nul

echo 检查EMS后端健康状态...
curl -f http://localhost:8081/api/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ EMS后端服务正常
) else (
    echo ⚠️  EMS后端服务可能还在启动中，请稍等片刻
)

echo 检查Node-RED服务...
curl -f http://localhost:1880/ >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Node-RED服务正常
) else (
    echo ⚠️  Node-RED服务可能还在启动中，请稍等片刻
)

echo.
echo ========================================
echo 🧪 下一步操作:
echo ========================================
echo 1. 访问 http://localhost:1880 查看Node-RED流程
echo 2. 注册测试设备:
echo    curl -X POST http://localhost:8081/api/devices ^
echo      -H "Content-Type: application/json" ^
echo      -d '{"deviceId":"RAD-001","deviceName":"测试辐射设备","deviceType":"RADIATION"}'
echo 3. 观察Node-RED调试面板的数据流
echo 4. 访问 http://localhost:8081/api/h2-console 查看数据库
echo ========================================

echo.
echo 实时查看日志命令:
echo   EMS后端:   docker-compose logs -f ems-backend
echo   Node-RED:  docker-compose logs -f nodered
echo   MQTT:      docker-compose logs -f mosquitto
echo.

echo 停止服务命令: docker-compose down
echo 重启服务命令: docker-compose restart
echo.

pause