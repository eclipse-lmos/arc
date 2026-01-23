#!/bin/bash

# SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
#
# SPDX-License-Identifier: Apache-2.0

# Quick test script for ADL validation API
# Usage: ./test-validation.sh

SERVER_URL="http://localhost:8080/graphql"

echo "Testing ADL Validation API..."
echo "================================"
echo ""

# Test 1: Valid ADL with tools and references
echo "Test 1: Valid ADL with tools and references"
curl -X POST "$SERVER_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { validate(adl: \"### UseCase: password_reset\n#### Description\nUser wants to reset password.\n#### Solution\nCall @reset_password() and go to #user_verification.\n----\n\") { syntaxErrors { line message } usedTools references } }"
  }' | jq '.'
echo ""
echo ""

# Test 2: ADL with syntax errors
echo "Test 2: ADL with syntax errors (unclosed bracket)"
curl -X POST "$SERVER_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { validate(adl: \"### UseCase: test\n#### Solution\nThis has [unclosed bracket\n----\n\") { syntaxErrors { line message } usedTools references } }"
  }' | jq '.'
echo ""
echo ""

# Test 3: ADL with multiple tools
echo "Test 3: ADL with multiple tools"
curl -X POST "$SERVER_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { validate(adl: \"### UseCase: multi\n#### Solution\nCall @tool1() and @tool2().\n----\n\") { syntaxErrors { line message } usedTools references } }"
  }' | jq '.'
echo ""
echo ""

echo "Tests completed!"
