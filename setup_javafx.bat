@echo off
chcp 65001 >nul
title JavaFX 环境设置工具

echo ========================================
echo        JavaFX 环境自动设置工具
echo ========================================
echo.

REM 检查管理员权限
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo ⚠  注意：如需设置系统环境变量，请以管理员身份运行此脚本
    echo.
)

REM 1. 检查并下载 JavaFX SDK
echo [1/4] 检查 JavaFX SDK...
if exist "javafx-sdk-21\lib\javafx.base.jar" (
    echo ✓ JavaFX SDK 已存在
) else (
    echo 正在下载 JavaFX SDK (Windows x64, 版本 21.0.1)...
    echo 文件大小约 50MB，请稍候...
    
    powershell -Command "Invoke-WebRequest -Uri 'https://download2.gluonhq.com/openjfx/21.0.1/openjfx-21.0.1_windows-x64_bin-sdk.zip' -OutFile 'javafx-sdk.zip' -UserAgent 'Mozilla/5.0'"
    
    if exist "javafx-sdk.zip" (
        echo 解压文件中...
        powershell -Command "Expand-Archive -Path 'javafx-sdk.zip' -DestinationPath '.' -Force"
        del /q javafx-sdk.zip
        if exist "javafx-sdk-21.0.1" (
            rename "javafx-sdk-21.0.1" "javafx-sdk-21"
        )
        echo ✓ JavaFX SDK 下载完成
    ) else (
        echo ✗ 下载失败，请检查网络连接
        pause
        exit /b 1
    )
)

REM 2. 设置项目环境变量
echo [2/4] 设置项目环境变量...
set JAVAFX_HOME=%CD%\javafx-sdk-21
echo ✓ JAVAFX_HOME=%JAVAFX_HOME%

REM 3. 更新系统 PATH（仅当前会话）
echo [3/4] 更新 PATH 环境变量...
set PATH=%JAVAFX_HOME%\bin;%PATH%

REM 4. 创建运行脚本
echo [4/4] 创建运行脚本...
(
echo @echo off
echo echo 运行 JavaFX 应用程序...
echo set JAVAFX_HOME=%JAVAFX_HOME%
echo set PATH=%%JAVAFX_HOME%%\bin;%%PATH%%
echo mvn clean compile javafx:run
echo pause
) > run_javafx.bat

echo.
echo ========================================
echo ✅ JavaFX 环境设置完成！
echo.
echo 运行方式：
echo 1. 双击 run_javafx.bat 运行应用程序
echo 2. 或使用命令: mvn clean compile javafx:run
echo.
echo 环境变量已为当前终端会话设置
echo 如需永久设置，请在系统环境变量中添加：
echo   JAVAFX_HOME = %JAVAFX_HOME%
echo   PATH 添加 = %%JAVAFX_HOME%%\bin
echo ========================================
echo.

REM 测试JavaFX是否可用
if exist "%JAVAFX_HOME%\bin\javafx.dll" (
    echo 📦 JavaFX 库位置: %JAVAFX_HOME%\lib\
    echo 📝 包含模块: javafx.base, javafx.controls, javafx.fxml, javafx.graphics, javafx.media, javafx.swing, javafx.web
) else (
    echo ⚠  警告：JavaFX 可能未正确安装
)

echo 按任意键退出...
pause >nul