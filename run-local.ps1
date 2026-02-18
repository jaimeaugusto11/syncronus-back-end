Write-Host "Iniciando em MODO LOCAL (Offline)..." -ForegroundColor Green

# Force use of IntelliJ JDK 21 to avoid Lombok incompatibility with Java 24
$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.1\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Write-Host "Using JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Cyan

$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_USERNAME = "root" 

# Passamos a senha vazia via argumento do Spring Boot para garantir que sobrescreva o padrao
# O padrao no application.yml e 'root', entao precisamos forcar vazio.
mvn clean spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.password="
