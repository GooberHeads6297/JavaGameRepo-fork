@echo off
setlocal

call powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0compile.ps1"
exit /b %errorlevel%
