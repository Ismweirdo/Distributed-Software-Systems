$ErrorActionPreference = "Stop"

$projectPath = "D:\CreatorProject\seckill-system"
$settingsFile = Join-Path $projectPath ".mvn-settings.xml"

if (-not (Test-Path $settingsFile)) {
@"
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <localRepository>D:/CreatorProject/seckill-system/.m2repo</localRepository>
</settings>
"@ | Set-Content -Path $settingsFile -Encoding UTF8
}

Set-Location $projectPath
Write-Host "正在编译并打包（禁用 Elasticsearch）..." -ForegroundColor Green

.\mvnw.cmd -s $settingsFile clean package -DskipTests -Dspring.profiles.active=no-es
if ($LASTEXITCODE -ne 0) {
    Write-Host "构建失败" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "构建成功，启动应用..." -ForegroundColor Green
.\mvnw.cmd -s $settingsFile spring-boot:run -Dspring.profiles.active=no-es
