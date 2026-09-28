# Chạy migration thuoc_tinh
# Yêu cầu: sqlcmd đã được cài đặt trong PATH
# Download: https://docs.microsoft.com/en-us/sql/tools/sqlcmd-utility

$ErrorActionPreference = "Stop"

# Cấu hình
$Server = "localhost"
$Database = "QLBanMayTinh"
$User = "sa"
$Password = "SaoClub@2024"
$ScriptPath = "$PSScriptRoot\QLBanMayTinh.sql"

Write-Host "🚀 Chạy script cơ sở dữ liệu..." -ForegroundColor Cyan
Write-Host "   Server: $Server"
Write-Host "   Database: $Database"
Write-Host "   Script: $ScriptPath"

# Kiểm tra sqlcmd
try {
    $sqlcmdVersion = & sqlcmd -? 2>&1 | Select-Object -First 1
    Write-Host "   sqlcmd: OK" -ForegroundColor Green
} catch {
    Write-Host "   sqlcmd: NOT FOUND!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Vui lòng cài đặt SQL Server Command Line Utilities:"
    Write-Host "  https://docs.microsoft.com/en-us/sql/tools/sqlcmd-utility"
    exit 1
}

# Chạy migration
Write-Host ""
Write-Host "⏳ Đang chạy migration..." -ForegroundColor Yellow

$env:SQLCMDPASSWORD = $Password
$result = & sqlcmd -S $Server -d $Database -U $User -P $Password -i $ScriptPath -b 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Migration hoàn tất thành công!" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "❌ Migration thất bại!" -ForegroundColor Red
    Write-Host $result
    exit 1
}
