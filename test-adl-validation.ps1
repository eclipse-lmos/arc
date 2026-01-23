# SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
#
# SPDX-License-Identifier: Apache-2.0
# Quick test wrapper - run from project root
# This script runs the ADL validation tests

Set-Location (Join-Path $PSScriptRoot "adl-server")
& .\test-all.ps1
Set-Location $PSScriptRoot

