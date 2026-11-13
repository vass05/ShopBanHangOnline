param([string]$OutputPath = '.env.production')
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$target = [IO.Path]::GetFullPath((Join-Path $root $OutputPath))
if (-not $target.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Environment file must stay inside the repository.' }
if (Test-Path -LiteralPath $target) { throw 'Environment file already exists; edit it without regenerating credentials for existing volumes.' }
$content = [IO.File]::ReadAllText((Join-Path $root '.env.production.example'))
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $content = [regex]::Replace($content, '(?m)^(MYSQL_PASSWORD|MYSQL_ROOT_PASSWORD|RABBITMQ_PASSWORD|JWT_SECRET)=GENERATE\r?$', {
        param($match)
        $bytes = New-Object byte[] 32
        $rng.GetBytes($bytes)
        $match.Groups[1].Value + '=' + ([BitConverter]::ToString($bytes).Replace('-','').ToLowerInvariant())
    })
    [IO.File]::WriteAllText($target, $content, [Text.UTF8Encoding]::new($false))
} finally { $rng.Dispose() }
Write-Host "Created $OutputPath. Keep this file private; configure SMTP and VNPAY before using those services."
