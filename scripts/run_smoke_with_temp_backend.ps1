param(
    [int]$Port = 18080
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptRoot ".."))
$backendDir = Join-Path $projectRoot "travel-backend"
$smokeScript = Join-Path $scriptRoot "full_api_smoke_test.ps1"
$logOut = Join-Path $projectRoot "test-output\backend-temp.out.log"
$logErr = Join-Path $projectRoot "test-output\backend-temp.err.log"
$baseUrl = "http://127.0.0.1:$Port"

if (Test-Path $logOut) { Remove-Item $logOut -Force }
if (Test-Path $logErr) { Remove-Item $logErr -Force }

$backendProc = $null
try {
    Write-Host "Starting backend at $baseUrl ..." -ForegroundColor Cyan
    $backendProc = Start-Process -FilePath "mvn" `
        -ArgumentList "spring-boot:run", "-Dspring-boot.run.arguments=--server.port=$Port" `
        -WorkingDirectory $backendDir `
        -RedirectStandardOutput $logOut `
        -RedirectStandardError $logErr `
        -PassThru

    $ready = $false
    for ($i = 0; $i -lt 240; $i++) {
        Start-Sleep -Seconds 1
        try {
            $health = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/destinations?page=1&size=1" -TimeoutSec 2
            if ($health.code -eq 200) {
                $ready = $true
                break
            }
        } catch {
            # keep waiting
        }
    }

    if (-not $ready) {
        Write-Host "Backend startup timed out. Check logs:" -ForegroundColor Red
        Write-Host $logOut -ForegroundColor Yellow
        Write-Host $logErr -ForegroundColor Yellow
        exit 1
    }

    Write-Host "Backend is ready, running smoke test..." -ForegroundColor Green
    & $smokeScript -BaseUrl $baseUrl
} finally {
    if ($null -ne $backendProc) {
        try {
            if (-not $backendProc.HasExited) {
                Stop-Process -Id $backendProc.Id -Force
            }
        } catch {
            # ignore cleanup error
        }
    }
}

