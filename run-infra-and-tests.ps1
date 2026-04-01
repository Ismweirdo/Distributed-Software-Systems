Param(
    [switch]$SkipComposeUp,
    [string]$BackendBaseUrl = "http://localhost:8084",
    [int]$BackendWaitSeconds = 180,
    [int]$PollIntervalSeconds = 3
)

$ErrorActionPreference = "Stop"

function Wait-HttpReady {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [Parameter(Mandatory = $true)]
        [int]$TimeoutSeconds,
        [int]$IntervalSeconds = 3
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $resp = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 5 -ErrorAction Stop
            if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 500) {
                return $true
            }
        } catch {
            # Keep polling until timeout.
        }
        Start-Sleep -Seconds $IntervalSeconds
    }

    return $false
}

$projectPath = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
Set-Location $projectPath

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "Docker command not found. Please install/start Docker Desktop first." -ForegroundColor Red
    exit 1
}

if (-not $SkipComposeUp) {
    Write-Host "Starting MySQL master/slaves + Redis + Elasticsearch + service containers..." -ForegroundColor Green
    docker compose up -d
}

Write-Host "Checking replication status..." -ForegroundColor Green
docker exec seckill-mysql-slave1 mysql -uroot -p123456 -e "SHOW REPLICA STATUS\G" | Select-String -Pattern "Replica_IO_Running|Replica_SQL_Running"
docker exec seckill-mysql-slave2 mysql -uroot -p123456 -e "SHOW REPLICA STATUS\G" | Select-String -Pattern "Replica_IO_Running|Replica_SQL_Running"

$healthUrl = "$BackendBaseUrl/api/health/init-status"
Write-Host "Waiting for backend readiness: $healthUrl" -ForegroundColor Green
if (-not (Wait-HttpReady -Url $healthUrl -TimeoutSeconds $BackendWaitSeconds -IntervalSeconds $PollIntervalSeconds)) {
    Write-Host "Backend did not become ready within $BackendWaitSeconds seconds." -ForegroundColor Red
    Write-Host "Tip: run 'docker logs seckill-service --tail 200' to inspect startup errors." -ForegroundColor Yellow
    exit 1
}

Write-Host "Triggering product sync to Elasticsearch..." -ForegroundColor Green
$syncUrl = "$BackendBaseUrl/api/search/sync"
Invoke-WebRequest -UseBasicParsing -Method POST -Uri $syncUrl -TimeoutSec 20 | Out-Null

if (-not (Test-Path .\mvnw.cmd)) {
    Write-Host "mvnw.cmd not found. Please run this script from project root." -ForegroundColor Red
    exit 1
}

Write-Host "Running read/write separation and search tests..." -ForegroundColor Green
& .\mvnw.cmd test "-Dtest=ReadWriteSeparationTest,SearchServiceTest"
exit $LASTEXITCODE
