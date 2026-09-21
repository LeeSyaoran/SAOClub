@echo off
REM ================================================
REM Chạy migration thuoc_tinh cho QLBanMayTinh
REM Yêu cầu: sqlcmd đã cài trong PATH
REM ================================================

setlocal enabledelayedexpansion

set "SERVER=localhost"
set "DATABASE=QLBanMayTinh"
set "USER=sa"
set "PASSWORD=SaoClub@2024"
set "SCRIPT=%~dp0Migrate_ThuocTinh.sql"

echo ================================================
echo  Migration: thuoc_tinh
echo ================================================
echo   Server: %SERVER%
echo   Database: %DATABASE%
echo   Script: %SCRIPT%
echo ================================================
echo.

REM Kiểm tra sqlcmd
sqlcmd -? >nul 2>&1
if errorlevel 1 (
    echo [ERROR] sqlcmd not found!
    echo Vui long cai dat SQL Server Command Line Utilities:
    echo   https://docs.microsoft.com/en-us/sql/tools/sqlcmd-utility
    pause
    exit /b 1
)

echo [OK] sqlcmd found
echo.
echo Running migration...

REM Chạy migration
sqlcmd -S %SERVER% -d %DATABASE% -U %USER% -P %PASSWORD% -i "%SCRIPT%" -b

if errorlevel 1 (
    echo.
    echo [ERROR] Migration failed!
    pause
    exit /b 1
)

echo.
echo [SUCCESS] Migration completed!
pause
