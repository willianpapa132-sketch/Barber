param(
    [string]$OwnerLogin = 'dono@plataforma.local',
    [string]$PlatformHost = 'localhost',
    [string]$BaseDomain = 'localhost'
)
$ErrorActionPreference = 'Stop'
$projectDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$envFile = Join-Path $projectDirectory '.env'
if (Test-Path -LiteralPath $envFile) { throw 'O arquivo .env ja existe. Atualize-o manualmente para preservar as chaves existentes.' }
foreach ($value in @($OwnerLogin, $PlatformHost, $BaseDomain)) {
    if ($value -match '[\r\n"''$`#\s]') { throw 'Informe e-mail e dominios validos, sem espacos.' }
}
$keyBytes = New-Object byte[] 32
$passwordBytes = New-Object byte[] 24
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($keyBytes); $rng.GetBytes($passwordBytes) } finally { $rng.Dispose() }
$key = [Convert]::ToBase64String($keyBytes)
$password = [Convert]::ToBase64String($passwordBytes)
$content = @"
APP_MULTITENANCY_ENABLED=true
APP_PLATFORM_HOST=$PlatformHost
APP_TENANT_BASE_DOMAIN=$BaseDomain
APP_PLATFORM_ADMIN_LOGIN=$OwnerLogin
APP_PLATFORM_ADMIN_PASSWORD=$password
APP_TENANT_ENCRYPTION_KEY=$key
"@
[IO.File]::WriteAllText($envFile, $content, [Text.UTF8Encoding]::new($false))
Write-Output "Configuracao criada em $envFile"
Write-Output "Painel: http://${PlatformHost}:8080/plataforma"
Write-Output 'O login, a senha gerada e a chave de criptografia estao no .env. Guarde uma copia segura desse arquivo.'
Write-Output 'Inicie com: docker compose up -d --build'
