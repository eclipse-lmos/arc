# Quick test script for ADL validation API (PowerShell)
# Usage: .\test-validation.ps1

$SERVER_URL = "http://localhost:8080/graphql"

Write-Host "Testing ADL Validation API..." -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green
Write-Host ""

# Test 1: Valid ADL with tools and references
Write-Host "Test 1: Valid ADL with tools and references" -ForegroundColor Yellow
$body1 = @{
    query = "mutation { validate(adl: \"### UseCase: password_reset`n#### Description`nUser wants to reset password.`n#### Solution`nCall @reset_password() and go to #user_verification.`n----`n\") { syntaxErrors { line message } usedTools references } }"
} | ConvertTo-Json

Invoke-RestMethod -Uri $SERVER_URL -Method Post -Body $body1 -ContentType "application/json" | ConvertTo-Json -Depth 10
Write-Host ""

# Test 2: ADL with syntax errors
Write-Host "Test 2: ADL with syntax errors (unclosed bracket)" -ForegroundColor Yellow
$body2 = @{
    query = "mutation { validate(adl: \"### UseCase: test`n#### Solution`nThis has [unclosed bracket`n----`n\") { syntaxErrors { line message } usedTools references } }"
} | ConvertTo-Json

Invoke-RestMethod -Uri $SERVER_URL -Method Post -Body $body2 -ContentType "application/json" | ConvertTo-Json -Depth 10
Write-Host ""

# Test 3: ADL with multiple tools
Write-Host "Test 3: ADL with multiple tools" -ForegroundColor Yellow
$body3 = @{
    query = "mutation { validate(adl: \"### UseCase: multi`n#### Solution`nCall @tool1() and @tool2().`n----`n\") { syntaxErrors { line message } usedTools references } }"
} | ConvertTo-Json

Invoke-RestMethod -Uri $SERVER_URL -Method Post -Body $body3 -ContentType "application/json" | ConvertTo-Json -Depth 10
Write-Host ""

Write-Host "Tests completed!" -ForegroundColor Green

