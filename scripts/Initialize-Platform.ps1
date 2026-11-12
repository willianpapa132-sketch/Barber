param(
    [string]$OwnerLogin = 'dono@plataforma.local',
    [string]$PlatformHost = 'localhost',
    [string]$BaseDomain = 'localhost'
)
$ErrorActionPreference = 'Stop'
$projectDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$envFile = Join-Path $projectDirectory '.env'
if (Test-Path -LiteralPath $envFile) { throw 'O arquivo .env ja existe. Atualize-o manualmente para preservar as credenciais existentes.' }
foreach ($value in @($OwnerLogin, $PlatformHost, $BaseDomain)) {
    if ($value -match '[\r\n"''$`#\s]') { throw 'Informe e-mail e dominios validos, sem espacos.' }
}
$passwordBytes = New-Object byte[] 24
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($passwordBytes) } finally { $rng.Dispose() }
$password = [Convert]::ToBase64String($passwordBytes)
$content = @"
APP_MULTITENANCY_ENABLED=true
APP_PLATFORM_HOST=$PlatformHost
APP_TENANT_BASE_DOMAIN=$BaseDomain
APP_PLATFORM_ADMIN_LOGIN=$OwnerLogin
APP_PLATFORM_ADMIN_PASSWORD=$password
"@
[IO.File]::WriteAllText($envFile, $content, [Text.UTF8Encoding]::new($false))
Write-Output "Configuracao criada em $envFile"
Write-Output "Painel: http://${PlatformHost}:8080/plataforma"
Write-Output 'O login e a senha gerada estao no .env. Guarde uma copia segura desse arquivo.'
Write-Output 'Inicie com: docker compose up -d --build'
