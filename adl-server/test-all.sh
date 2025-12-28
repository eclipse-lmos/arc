#!/bin/bash

# One-command test script for Linux/Mac
# This script builds, runs tests, starts server, and runs integration tests

echo "========================================"
echo "ADL Validation API - Complete Test Suite"
echo "========================================"
echo ""

# Step 1: Build and run unit tests
echo "Step 1: Building and running unit tests..."
./gradlew :adl-server:test --no-daemon
if [ $? -ne 0 ]; then
    echo "Unit tests failed!"
    exit 1
fi
echo "Unit tests passed!"
echo ""

# Step 2: Build the application
echo "Step 2: Building application..."
./gradlew :adl-server:build --no-daemon
if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi
echo "Build successful!"
echo ""

# Step 3: Start server in background
echo "Step 3: Starting server..."
./gradlew :adl-server:run --no-daemon &
SERVER_PID=$!

# Wait for server to start
echo "Waiting for server to start..."
sleep 5

# Step 4: Run integration tests
echo "Step 4: Running integration tests..."
chmod +x test-validation.sh
./test-validation.sh

# Step 5: Stop server
echo "Stopping server..."
kill $SERVER_PID 2>/dev/null

echo ""
echo "========================================"
echo "All tests completed!"
echo "========================================"

