$ErrorActionPreference = "Stop"

param(
    [switch]$SkipBuild,
    [switch]$SkipTests
)

$projectPath = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
$settingsFile = Join-Path $projectPath ".mvn-settings.xml"
$localRepo = (Join-Path $projectPath ".m2repo").Replace("\", "/")

if (-not (Test-Path (Join-Path $projectPath "mvnw.cmd"))) {
    Write-Host "未找到 mvnw.cmd，请在项目根目录执行脚本。" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $settingsFile)) {
@"
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <localRepository>$localRepo</localRepository>
</settings>
"@ | Set-Content -Path $settingsFile -Encoding UTF8
}

Set-Location $projectPath

if (-not $SkipBuild) {
    $buildArgs = @("-s", $settingsFile, "clean", "package", "-Dspring.profiles.active=no-es")
    if ($SkipTests) {
        $buildArgs += "-DskipTests"
    }
    Write-Host "正在构建项目（profile=no-es）..." -ForegroundColor Green
    & .\mvnw.cmd @buildArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Host "构建失败，请根据日志排查后重试。" -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

Write-Host "正在启动应用（profile=no-es）..." -ForegroundColor Green
& .\mvnw.cmd -s $settingsFile spring-boot:run -Dspring-boot.run.profiles=no-es
