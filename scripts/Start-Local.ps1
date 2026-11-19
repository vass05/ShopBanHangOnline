param([switch]$NoBuild)
# This command never seeds SQL, resets volumes or regenerates existing credentials.
$ErrorActionPreference = 'Continue'
$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    & docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Open Docker Desktop, wait until it is ready, then run this command again.' }

    $config = Join-Path $root '.env'
    if (-not (Test-Path -LiteralPath $config)) {
        $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
        try {
            $bytes = New-Object byte[] 32
            $rng.GetBytes($bytes)
            $jwt = [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
        } finally { $rng.Dispose() }
        # RabbitMQ stores its node identity in the volume: retain the old hostname.
        $hostname = & docker inspect --format '{{.Config.Hostname}}' eshop_rabbitmq 2>$null
        if ($LASTEXITCODE -ne 0) { $hostname = 'rabbitmq' }
        if ($hostname -notmatch '^[a-zA-Z0-9_-]+$') { throw 'Unexpected RabbitMQ hostname; inspect existing container before proceeding.' }
        [IO.File]::WriteAllText($config, "JWT_SECRET=$jwt`nRABBITMQ_HOSTNAME=$hostname`n", [Text.UTF8Encoding]::new($false))
    }
    $gmail = Join-Path $root '.env.gmail.local'
    if (-not (Test-Path -LiteralPath $gmail)) { Copy-Item -LiteralPath '.env.gmail.example' -Destination $gmail }
    # Print only configuration readiness, never passwords or the rendered Compose config.
    $mailConfig = [IO.File]::ReadAllText($gmail)
    if ($mailConfig -notmatch '(?m)^SMTP_USERNAME=\S+@gmail\.com\s*$' -or $mailConfig -notmatch '(?m)^SMTP_PASSWORD=\S+\s*$') {
        Write-Warning 'Fill SMTP_USERNAME and SMTP_PASSWORD (Google App Password) in .env.gmail.local to deliver real registration OTP. Re-run with -NoBuild after saving.'
    }
    $others = @(& docker ps --filter 'label=com.docker.compose.project=helishop-integration' --format '{{.ID}}')
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect the integration project.' }
    if ($others.Count -gt 0) {
        foreach ($id in $others) { if ($id -notmatch '^[a-f0-9]{12,64}$') { throw 'Unexpected container ID.' } }
        Write-Host 'Stopping integration containers; all integration volumes are retained.'
        & docker stop @others
        if ($LASTEXITCODE -ne 0) { throw 'Could not stop integration containers.' }
    }
    $buildArgs = @('--build')
    if ($NoBuild) { $buildArgs = @() }
    & docker compose --project-name webbnhngonline -f docker-compose.yml up -d @buildArgs --wait --wait-timeout 240
    if ($LASTEXITCODE -ne 0) { throw 'Local stack failed. Inspect: docker compose logs --tail 80 backend' }
    Write-Host 'Ready: Docker web http://localhost ; API http://localhost:8080 ; npm run dev http://localhost:5173'
} finally { Pop-Location }
