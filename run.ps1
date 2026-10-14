# FreshRoute One-Click PowerShell Launcher
Write-Host "======================================================================" -ForegroundColor Green
Write-Host "Starting FreshRoute System (Problem Statement 03 - Hackathon 2026)" -ForegroundColor Cyan
Write-Host "Stack: Java 21, Spring Boot 3, PostgreSQL, React 18, Vite, Tailwind CSS" -ForegroundColor DarkGray
Write-Host "======================================================================" -ForegroundColor Green

$Root = Split-Path -Parent $MyInvocation.MyCommand.Definition

Write-Host "`n[1/2] Launching Spring Boot Backend on http://localhost:8080..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$Root\backend'; mvn spring-boot:run"

Write-Host "[2/2] Launching Vite React Frontend on http://localhost:5173..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$Root\frontend'; npm run dev"

Start-Sleep -Seconds 4

Write-Host "`nOpening FreshRoute Command Center in your default browser..." -ForegroundColor Green
Start-Process "http://localhost:5173"

Write-Host "`n======================================================================" -ForegroundColor Green
Write-Host "System Active!" -ForegroundColor Green
Write-Host "- Frontend Dashboard: http://localhost:5173" -ForegroundColor White
Write-Host "- Backend REST API:   http://localhost:8080" -ForegroundColor White
Write-Host "- Swagger OpenAPI UI: http://localhost:8080/swagger-ui/index.html" -ForegroundColor White
Write-Host "======================================================================" -ForegroundColor Green
