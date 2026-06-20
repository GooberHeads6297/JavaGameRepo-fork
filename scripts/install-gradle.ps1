param(
    [string]$Version = $(if ($env:GRADLE_VERSION) { $env:GRADLE_VERSION } else { '9.2.0' })
)

$ErrorActionPreference = 'Stop'
$MinMajor = if ($env:GRADLE_MIN_MAJOR) { [int]$env:GRADLE_MIN_MAJOR } else { 8 }

function Get-GradleMajorVersion {
    param([string]$GradleExe)

    $output = & $GradleExe -v 2>$null
    foreach ($line in $output) {
        if ($line -match '^Gradle\s+([0-9]+)\.') {
            return [int]$Matches[1]
        }
    }

    return 0
}

$installRoot = if ($env:GRADLE_INSTALL_ROOT) {
    $env:GRADLE_INSTALL_ROOT
} else {
    Join-Path $env:LOCALAPPDATA 'Gradle'
}

$installDir = Join-Path $installRoot "gradle-$Version"
$binDir = Join-Path $installDir 'bin'
$archiveUrl = "https://services.gradle.org/distributions/gradle-$Version-bin.zip"
$tempRoot = Join-Path $env:TEMP "gradle-install-$Version"
$archivePath = Join-Path $tempRoot "gradle-$Version-bin.zip"
$extractDir = Join-Path $tempRoot 'extract'

if (Get-Command gradle -ErrorAction SilentlyContinue) {
    $existing = (Get-Command gradle).Source
    if ((Get-GradleMajorVersion -GradleExe $existing) -ge $MinMajor) {
        Write-Host "Gradle is already available at: $existing"
        gradle -v
        exit 0
    }
}

New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null
New-Item -ItemType Directory -Force -Path $extractDir | Out-Null
New-Item -ItemType Directory -Force -Path $installRoot | Out-Null

Invoke-WebRequest -Uri $archiveUrl -OutFile $archivePath
Expand-Archive -Path $archivePath -DestinationPath $extractDir -Force

if (Test-Path $installDir) {
    Remove-Item -Recurse -Force $installDir
}

Move-Item -Path (Join-Path $extractDir "gradle-$Version") -Destination $installDir
Remove-Item -Recurse -Force $tempRoot

$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
$pathParts = @()
if ($userPath) {
    $pathParts = $userPath -split ';' | Where-Object { $_ }
}

if ($pathParts -notcontains $binDir) {
    $updatedPath = (@($binDir) + $pathParts) -join ';'
    [Environment]::SetEnvironmentVariable('Path', $updatedPath, 'User')
    $env:Path = "$binDir;$env:Path"
} else {
    $env:Path = "$binDir;$env:Path"
}

Write-Host "Gradle installed to: $installDir"
Write-Host "Gradle bin added to user PATH: $binDir"
Write-Host "Open a new terminal window to pick up the updated PATH."
& (Join-Path $binDir 'gradle.bat') -v
