# SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
#
# SPDX-License-Identifier: Apache-2.0
# One-command test script for Windows
# This script builds, runs tests, starts server, and runs integration tests

# Get the root directory (parent of adl-server)
$rootDir = Split-Path -Parent $PSScriptRoot
$originalDir = Get-Location

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ADL Validation API - Complete Test Suite" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Change to root directory
Set-Location $rootDir

# Step 1: Build and run unit tests
Write-Host "Step 1: Building and running unit tests..." -ForegroundColor Yellow
& .\gradlew.bat :adl-server:test --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "Unit tests failed!" -ForegroundColor Red
    Set-Location $originalDir
    exit 1
}
Write-Host "Unit tests passed!" -ForegroundColor Green
Write-Host ""

# Step 2: Build the application
Write-Host "Step 2: Building application..." -ForegroundColor Yellow
& .\gradlew.bat :adl-server:build --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    Set-Location $originalDir
    exit 1
}
Write-Host "Build successful!" -ForegroundColor Green
Write-Host ""

# Step 3: Start server in background
Write-Host "Step 3: Starting server..." -ForegroundColor Yellow
$serverJob = Start-Job -ScriptBlock {
    Set-Location $using:rootDir
    & .\gradlew.bat :adl-server:run --no-daemon
}

# Wait for server to start
Write-Host "Waiting for server to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Step 4: Run integration tests
Write-Host "Step 4: Running integration tests..." -ForegroundColor Yellow
Set-Location (Join-Path $rootDir "adl-server")
& .\test-validation.ps1

# Step 5: Stop server
Write-Host "Stopping server..." -ForegroundColor Yellow
Stop-Job $serverJob
Remove-Job $serverJob

# Restore original directory
Set-Location $originalDir

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "All tests completed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

