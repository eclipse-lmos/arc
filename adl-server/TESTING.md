<!--
SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others

SPDX-License-Identifier: CC-BY-4.0
-->
# Testing the ADL Validation API

This guide provides multiple ways to test the validation API.

## Quick Test (One Command)

### Option 1: Run Unit Tests
```bash
./gradlew :adl-server:test
```

This runs all unit tests including the validation mutation tests.

### Option 2: Manual Testing with Scripts

#### On Linux/Mac:
```bash
# Make script executable
chmod +x test-validation.sh

# Start the server in one terminal
./gradlew :adl-server:run

# In another terminal, run tests
./test-validation.sh
```

#### On Windows (PowerShell):
```powershell
# Start the server in one terminal
.\gradlew.bat :adl-server:run

# In another terminal, run tests
.\test-validation.ps1
```

## Manual Testing with GraphiQL

1. Start the server:
   ```bash
   ./gradlew :adl-server:run
   ```

2. Open GraphiQL in your browser:
   ```
   http://localhost:8080/graphiql
   ```

3. Run this mutation:
   ```graphql
   mutation {
     validate(
       adl: """
       ### UseCase: password_reset
       #### Description
       User wants to reset their password.

       #### Solution
       Call @reset_password() and go to use case #user_verification.
       
       ----
       """
     ) {
       syntaxErrors {
         line
         message
       }
       usedTools
       references
     }
   }
   ```

## Manual Testing with cURL

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { validate(adl: \"### UseCase: test\n#### Solution\nCall @my_tool() and go to #usecase_ref.\n----\n\") { syntaxErrors { line message } usedTools references } }"
  }'
```

## Test Cases Covered

The unit tests cover:
- ✅ Valid ADL parsing
- ✅ Syntax error detection (unclosed brackets, quotes)
- ✅ Tool extraction from `@function()` calls
- ✅ Reference extraction from `#usecase_id` patterns
- ✅ Multiple tools and references
- ✅ Mixed tabs/spaces detection
- ✅ Fallback extraction when parsing fails
- ✅ Empty ADL handling

## Expected Results

### Valid ADL:
```json
{
  "data": {
    "validate": {
      "syntaxErrors": [],
      "usedTools": ["reset_password"],
      "references": ["user_verification"]
    }
  }
}
```

### ADL with Errors:
```json
{
  "data": {
    "validate": {
      "syntaxErrors": [
        {
          "line": 3,
          "message": "Unclosed '['"
        }
      ],
      "usedTools": [],
      "references": []
    }
  }
}
```

