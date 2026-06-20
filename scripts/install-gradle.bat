@echo off
setlocal

set "PS_EXE="
where pwsh >nul 2>nul
if %errorlevel% equ 0 set "PS_EXE=pwsh"
if not defined PS_EXE (
    where powershell >nul 2>nul
    if %errorlevel% equ 0 set "PS_EXE=powershell"
)

if not defined PS_EXE (
    echo PowerShell is required to install Gradle.
    exit /b 1
)

"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-gradle.ps1" %*
