# ==============================================================================
# SAOClub - Docker Emergency Cleaner & Restarter
# Chay script nay khi Docker bi treo, deadlock hoac khong the dung lai
# ==============================================================================

Write-Host "=== 1. Dang dong cac tien trinh Docker bi treo... ===" -ForegroundColor Yellow
Get-Process docker, docker-compose -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

Write-Host "=== 2. Thu don dep container bang Docker Compose (timeout 5s)... ===" -ForegroundColor Yellow
try {
    & docker compose down --timeout 5
} catch {
    Write-Host "Docker daemon khong phan hoi, bo qua compose down..." -ForegroundColor DarkYellow
}

Write-Host "=== 3. Buoc dung Docker Desktop va backend... ===" -ForegroundColor Yellow
Get-Process *docker* -ErrorAction SilentlyContinue | ForEach-Object {
    Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
}

Write-Host "=== 4. Shutdown WSL2 de giai phong RAM va tat ca Ports (1433, 8080, 5173, 11434)... ===" -ForegroundColor Yellow
& wsl --shutdown

Start-Sleep -Seconds 2

Write-Host "=== 5. Khoi dong lai Docker Desktop... ===" -ForegroundColor Cyan
$dockerPath = "$env:LOCALAPPDATA\Programs\DockerDesktop\Docker Desktop.exe"
if (Test-Path $dockerPath) {
    Start-Process -FilePath $dockerPath
} else {
    Write-Host "Khong tim thay Docker Desktop tai $dockerPath. Vui long mo Docker Desktop thu cong." -ForegroundColor Red
}

Write-Host "=== 6. Dang doi Docker Engine khoi dong hoan tat... ===" -ForegroundColor Cyan
$maxAttempts = 30
$attempt = 0
$ready = $false

while ($attempt -lt $maxAttempts) {
    $attempt++
    Start-Sleep -Seconds 3
    $check = & docker info 2>&1
    if ($LASTEXITCODE -eq 0) {
        $ready = $true
        break
    }
    Write-Host "  ... Dang khoi dong ($attempt/$maxAttempts)..." -ForegroundColor Gray
}

if ($ready) {
    Write-Host "=== DOCKER DA SAN SANG! ===" -ForegroundColor Green
    Write-Host "Ban co the chay: docker compose up -d" -ForegroundColor Green
} else {
    Write-Host "Docker can them thoi gian. Vui long kiem tra bieu tuong Docker Desktop o khay he thong (system tray)." -ForegroundColor Yellow
}
