@echo off
setlocal

call "%~dp0compile.bat"
if errorlevel 1 exit /b %errorlevel%

cd /d "%~dp0.."
java -jar "dist\xenoverse-portable.jar" %*
exit /b %errorlevel%
