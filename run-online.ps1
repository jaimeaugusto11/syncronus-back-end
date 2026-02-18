param (
    [string]$EC2_IP
)

if ([string]::IsNullOrEmpty($EC2_IP)) {
    $EC2_IP = Read-Host "Digite o IP Público da sua EC2"
}

if ([string]::IsNullOrEmpty($EC2_IP)) {
    Write-Host "Erro: IP da EC2 é obrigatório para o modo online." -ForegroundColor Red
    exit 1
}

Write-Host "Iniciando em MODO ONLINE (Conectando a $EC2_IP)..." -ForegroundColor Cyan
Write-Host "IMPORTANTE: Certifique-se que a porta 3306 está aberta no Security Group da AWS." -ForegroundColor Yellow

$env:DB_HOST = $EC2_IP
$env:DB_PORT = "3306"
# Estas são as credenciais definidas no docker-compose.yml da EC2
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "root"

mvn spring-boot:run
