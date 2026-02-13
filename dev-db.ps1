<#
.SYNOPSIS
    Script de controle para o PostgreSQL Local (Docker)
.DESCRIPTION
    Gerencia o container de banco de dados para desenvolvimento local.
.EXAMPLE
    .\dev-db.ps1 start
    .\dev-db.ps1 stop
    .\dev-db.ps1 logs
#>

param (
    [Parameter(Mandatory=$true)]
    [ValidateSet("start", "stop", "restart", "logs", "status", "clean")]
    [string]$Action
)

$ComposeFile = "docker-compose.dev.yml"

switch ($Action) {
    "start" {
        Write-Host "Iniciando PostgreSQL Local..." -ForegroundColor Green
        docker-compose -f $ComposeFile up -d
        Write-Host "Aguardando banco ficar pronto..." -ForegroundColor Cyan
        Start-Sleep -Seconds 3
        docker-compose -f $ComposeFile ps
    }
    "stop" {
        Write-Host "Parando PostgreSQL Local..." -ForegroundColor Yellow
        docker-compose -f $ComposeFile stop
    }
    "restart" {
        Write-Host "Reiniciando..." -ForegroundColor Yellow
        docker-compose -f $ComposeFile restart
    }
    "logs" {
        docker-compose -f $ComposeFile logs -f
    }
    "status" {
        docker-compose -f $ComposeFile ps
    }
    "clean" {
        Write-Host "ATENÇÃO: Isso apagará o banco e os dados!" -ForegroundColor Red
        $confirmation = Read-Host "Tem certeza? (s/N)"
        if ($confirmation -eq 's') {
            docker-compose -f $ComposeFile down -v
            Write-Host "Ambiente limpo." -ForegroundColor Green
        }
    }
}
