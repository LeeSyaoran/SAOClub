# Backup tu dong cho database QLBanMayTinh (chay trong container SQL Server qua Docker Compose).
# Duoc goi hang ngay boi Windows Task Scheduler (xem huong dan dang ky task o cuoi file nay).
#
# Cach hoat dong: BACKUP DATABASE la lenh chay BEN TRONG tien trinh SQL Server (trong container),
# nen duong dan dich (TO DISK) phai la duong dan THEO HE THONG FILE CUA CONTAINER (/backups/...),
# khong phai duong dan Windows — container da duoc mount ./Database/backups (host) <-> /backups
# (container) trong docker-compose.yml, nen file .bak se tu dong xuat hien o Database/backups/
# tren may that, du lenh backup thuc thi ben trong container.
#
# ponytail: chi luu local (may nay), chua tu dong day len cloud/o ngoai — neu can chong mat may/
# o dia, dinh ky tu chep thu muc Database/backups/ len Google Drive/OneDrive/USB rieng.

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repoRoot '.env'
$backupDir = Join-Path $PSScriptRoot 'backups'
$logFile = Join-Path $backupDir 'backup.log'
$retentionDays = 14

function Write-Log([string]$msg) {
    $line = "[{0}] {1}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $msg
    Write-Host $line
    Add-Content -Path $logFile -Value $line
}

try {
    if (-not (Test-Path $backupDir)) { New-Item -ItemType Directory -Path $backupDir -Force | Out-Null }

    $dbPassLine = Get-Content $envFile | Where-Object { $_ -match '^DB_PASSWORD=' } | Select-Object -First 1
    if (-not $dbPassLine) { throw "Khong tim thay DB_PASSWORD trong $envFile" }
    $dbPass = $dbPassLine.Substring('DB_PASSWORD='.Length).Trim()

    $stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
    $containerBakPath = "/backups/QLBanMayTinh_$stamp.bak"
    $hostBakPath = Join-Path $backupDir "QLBanMayTinh_$stamp.bak"

    Write-Log "Bat dau backup -> $hostBakPath"

    # COMPRESSION doi hoi SQL Server Standard/Enterprise tro len — Express (ban dang dung, xem
    # MSSQL_PID: Express trong docker-compose.yml) khong ho tro, phai bo tuy chon nay.
    $query = "BACKUP DATABASE QLBanMayTinh TO DISK = N'$containerBakPath' WITH INIT, STATS = 25;"
    Invoke-Sqlcmd -ServerInstance 'localhost,1433' -Username 'sa' -Password $dbPass -Query $query -QueryTimeout 600 -ErrorAction Stop

    if (Test-Path $hostBakPath) {
        $sizeMb = [math]::Round((Get-Item $hostBakPath).Length / 1MB, 1)
        Write-Log "Backup thanh cong ($sizeMb MB)"
    } else {
        throw "Lenh BACKUP chay xong nhung khong thay file tai $hostBakPath - kiem tra lai mount /backups trong docker-compose.yml"
    }

    $cutoff = (Get-Date).AddDays(-$retentionDays)
    $old = Get-ChildItem -Path $backupDir -Filter '*.bak' | Where-Object { $_.LastWriteTime -lt $cutoff }
    foreach ($f in $old) {
        Remove-Item $f.FullName -Force
        Write-Log "Da xoa backup cu (qua $retentionDays ngay): $($f.Name)"
    }
} catch {
    Write-Log "LOI: $($_.Exception.Message)"
    throw
}

<#
── Dang ky chay tu dong hang ngay (chay 1 lan, KHONG can quyen admin) ──────────────────────
Mo PowerShell trong thu muc project roi chay:

  $action  = New-ScheduledTaskAction -Execute 'powershell.exe' `
               -Argument '-NoProfile -ExecutionPolicy Bypass -File "d:\project code\SAOClub\Database\backup-db.ps1"'
  $trigger = New-ScheduledTaskTrigger -Daily -At 2:00AM
  $settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -DontStopOnIdleEnd
  Register-ScheduledTask -TaskName 'SAOClub-DB-Backup' -Action $action -Trigger $trigger -Settings $settings -Description 'Backup hang ngay database QLBanMayTinh (SAOClub)'

Kiem tra da dang ky: Get-ScheduledTask -TaskName 'SAOClub-DB-Backup'
Chay thu ngay:        Start-ScheduledTask -TaskName 'SAOClub-DB-Backup'
Huy dang ky:           Unregister-ScheduledTask -TaskName 'SAOClub-DB-Backup' -Confirm:$false
#>
