# Quick test - just run unit tests
# Usage: .\quick-test.ps1

$rootDir = Split-Path -Parent $PSScriptRoot
Set-Location $rootDir

Write-Host "Running ADL Validation Unit Tests..." -ForegroundColor Cyan
Write-Host ""

# Run tests with explicit JVM target
& .\gradlew.bat :adl-server:test --no-daemon -Pkotlin.jvm.target.validation.mode=warning

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Tests passed!" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "Tests failed!" -ForegroundColor Red
    exit 1
}

