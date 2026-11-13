param([switch]$NoBuild)
# Windows PowerShell 5 turns Docker's normal stderr progress into error records.
# Native commands are checked by exit code explicitly below.
$ErrorActionPreference = 'Continue'
$OutputEncoding = [Text.UTF8Encoding]::new($false)
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root
$path = Join-Path $root '.env.integration.local'
if (-not (Test-Path -LiteralPath $path)) {
    & "$PSScriptRoot/Initialize-Environment.ps1" -OutputPath '.env.integration.local'
    $content = [IO.File]::ReadAllText($path)
    $content = $content.Replace('helishop-prod','helishop-integration').Replace('helishop_prod','helishop_integration').Replace('eshop_prod','helishop_integration')
    $content = $content.Replace('FRONTEND_PORT=80','FRONTEND_PORT=18000').Replace('BACKEND_PORT=8080','BACKEND_PORT=18080')
    [IO.File]::WriteAllText($path, $content, [Text.UTF8Encoding]::new($false))
}
$content = [IO.File]::ReadAllText($path)
foreach ($expected in @('COMPOSE_PROJECT_NAME=helishop-integration','CONTAINER_PREFIX=helishop_integration','MYSQL_VOLUME_NAME=helishop_integration_mysql_data','REDIS_VOLUME_NAME=helishop_integration_redis_data','RABBITMQ_VOLUME_NAME=helishop_integration_rabbitmq_data','UPLOADS_VOLUME_NAME=helishop_integration_uploads')) {
    if ($content -notmatch ('(?m)^' + [regex]::Escape($expected) + '\r?$')) { throw "Integration isolation guard: expected $expected in .env.integration.local" }
}
$compose = @('compose','--env-file','.env.integration.local','-f','docker-compose.production.yml','-f','docker-compose.e2e.yml')
$buildArgs = @()
if (-not $NoBuild) { $buildArgs = @('--build') }
& docker @compose up -d @buildArgs --wait --wait-timeout 240
if ($LASTEXITCODE -ne 0) { throw 'Integration stack failed to start' }
[IO.File]::ReadAllText("$PSScriptRoot/fixtures/catalog-e2e.sql") | & docker @compose exec -T mysql sh -c 'exec mysql --default-character-set=utf8mb4 -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'
if ($LASTEXITCODE -ne 0) { throw 'Integration fixture failed' }
# The fixture may replace product stock; discard only this isolated stack's catalog cache.
& docker @compose exec -T redis redis-cli DEL 'products::9001' 'products::9002' 'categories::tree' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Could not invalidate fixture cache' }
Write-Host 'Integration ready: http://localhost:18000 ; captured mail: http://localhost:18025'
