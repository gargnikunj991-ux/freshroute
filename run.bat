@echo off
title FreshRoute - Perishable Produce Decision Support Platform
echo ======================================================================
echo Starting FreshRoute System (Problem Statement 03 - Hackathon 2026)
echo Stack: Java 21, Spring Boot 3, PostgreSQL, React 18, Vite, Tailwind CSS
echo ======================================================================
echo.

echo [1/2] Launching Spring Boot Backend on http://localhost:8080 ...
start "FreshRoute Backend (Spring Boot 3)" cmd /k "cd /d %~dp0backend && mvn spring-boot:run"

echo [2/2] Launching Vite React Frontend on http://localhost:5173 ...
start "FreshRoute Frontend (Vite React)" cmd /k "cd /d %~dp0frontend && npm run dev"

echo.
echo Waiting for servers to initialize...
timeout /t 4 >nul

echo Opening FreshRoute Command Center in your default browser...
start http://localhost:5173

echo.
echo ======================================================================
echo System successfully launched!
echo - Frontend Dashboard: http://localhost:5173
echo - Backend REST API:   http://localhost:8080
echo - Swagger OpenAPI UI: http://localhost:8080/swagger-ui/index.html
echo ======================================================================
pause
