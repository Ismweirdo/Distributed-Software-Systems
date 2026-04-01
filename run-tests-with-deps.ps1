Param(
    [switch]$SkipStartContainers,
    [int]$RedisPort = 6379
)

# Simple helper script to start Redis and MySQL Docker containers (idempotent) and run Maven tests via the project's wrapper.
# Usage: .\run-tests-with-deps.ps1  (run and start containers)
#        .\run-tests-with-deps.ps1 -SkipStartContainers  (only run mvnw.cmd tests)

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker command not found. Please install Docker and ensure it's in PATH."
    exit 1
}

function Ensure-ContainerRunning($name, $createArgs){
    $exists = docker ps -a --format "{{.Names}}" | Select-String -Pattern "^$name$" -Quiet
    if (-not $exists) {
        Write-Host "Creating and starting container: $name"
        Invoke-Expression "docker run -d --name $name $createArgs"
    } else {
        $running = (docker inspect -f '{{.State.Running}}' $name) -eq 'true'
        if (-not $running) {
            Write-Host "Starting existing container: $name"
            docker start $name | Out-Null
        } else {
            Write-Host "Container $name already running"
        }
    }
}

if (-not $SkipStartContainers) {
    # Redis
    Ensure-ContainerRunning -name 'seckill-redis' -createArgs " -p $RedisPort:6379 redis:6.2"

    # detect actual host mapping for redis and update RedisPort so tests use correct host port
    try {
        $portInfo = docker port seckill-redis 6379 2>$null
        if ($portInfo) {
            $hostPort = ($portInfo -split ':')[-1].Trim()
            if ($hostPort -match '^[0-9]+$') { $RedisPort = [int]$hostPort }
            Write-Host "Detected Redis mapped host port: $RedisPort"
        }
    } catch {}

    # MySQL (8) with native auth plugin for compatibility
    # Map to host 3307 to avoid collisions with local MySQL instances
    Ensure-ContainerRunning -name 'seckill-mysql' -createArgs " -e MYSQL_ROOT_PASSWORD=123456 -e MYSQL_DATABASE=seckill_db -p 3307:3306 mysql:8 --default-authentication-plugin=mysql_native_password"

    # Elasticsearch (single-node for tests)
    Ensure-ContainerRunning -name 'seckill-elasticsearch' -createArgs " -p 9200:9200 -e discovery.type=single-node -e ES_JAVA_OPTS='-Xms512m -Xmx512m' elasticsearch:7.17"

    # Wait for Redis readiness (try both possible container names)
    Write-Host "Waiting for Redis to be ready on host port $RedisPort..."
    $ready = $false
    for ($i=0; $i -lt 30; $i++){
        try {
            $pong = docker exec seckill-redis redis-cli PING 2>$null
            if ($pong -eq 'PONG') { $ready = $true; break }
        } catch {}
        try {
            $pong2 = docker exec seckill-redis-alt redis-cli PING 2>$null
            if ($pong2 -eq 'PONG') { $ready = $true; break }
        } catch {}
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { Write-Warning "Redis did not become ready within timeout." }

    # Wait for MySQL readiness
    Write-Host "Waiting for MySQL to be ready... (this may take ~20s)"
    $ready = $false
    for ($i=0; $i -lt 60; $i++){
        try {
            docker exec seckill-mysql mysql -uroot -p123456 -e "SELECT 1;" > $null 2>&1
            if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        } catch {}
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { Write-Warning "MySQL did not become ready within timeout." }

    # Wait for Elasticsearch readiness (host port 9200)
    Write-Host "Waiting for Elasticsearch to be ready on host port 9200..."
    $ready = $false
    for ($i=0; $i -lt 60; $i++){
        try {
            $resp = Invoke-WebRequest -UseBasicParsing -Uri http://localhost:9200 -TimeoutSec 2 -ErrorAction Stop
            if ($resp.StatusCode -eq 200) { $ready = $true; break }
        } catch {}
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { Write-Warning "Elasticsearch did not become ready within timeout." }
}

# Run Maven tests using project's wrapper if present
Write-Host "Running Maven tests..."
# Export environment variables so forked JVM/test processes also see Redis port
$env:SPRING_DATA_REDIS_PORT = $RedisPort
$env:SPRING_REDIS_PORT = $RedisPort
# Enable Elasticsearch repositories (application.yml reads 'elasticsearch.enabled')
$env:ELASTICSEARCH_ENABLED = 'true'
if (Test-Path .\mvnw.cmd) {
    Write-Host "Running mvnw.cmd with -Dspring.data.redis.port=$RedisPort"
    & .\mvnw.cmd "-Dspring.data.redis.port=$RedisPort" "-Dspring.redis.port=$RedisPort" "-Delasticsearch.enabled=true" "-Dspring.data.elasticsearch.repositories.enabled=true" test
    $code = $LASTEXITCODE
    if ($code -ne 0) { Write-Error "mvnw.cmd exited with code $code"; exit $code }
} elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
    Write-Host "Running mvn with -Dspring.data.redis.port=$RedisPort"
    mvn "-Dspring.data.redis.port=$RedisPort" "-Dspring.redis.port=$RedisPort" "-Delasticsearch.enabled=true" "-Dspring.data.elasticsearch.repositories.enabled=true" test
    exit $LASTEXITCODE
} else {
    Write-Error "Neither mvnw.cmd nor mvn found. Install Maven or use project wrapper."
    exit 1
}

Write-Host "Tests completed successfully."

Write-Host "Tests completed successfully."