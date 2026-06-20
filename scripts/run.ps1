$ErrorActionPreference = 'Stop'

$rootDir = Split-Path -Parent $PSScriptRoot
& "$PSScriptRoot\compile.ps1"

$jarPath = Join-Path $rootDir 'dist/xenoverse-portable.jar'
Set-Location $rootDir
& java -jar $jarPath @args
exit $LASTEXITCODE
