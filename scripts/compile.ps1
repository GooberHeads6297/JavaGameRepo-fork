param()

$ErrorActionPreference = 'Stop'

$rootDir = Split-Path -Parent $PSScriptRoot
Set-Location $rootDir

$gradleVersion = if ($env:GRADLE_VERSION) { $env:GRADLE_VERSION } else { '9.2.0' }
$minMajor = if ($env:GRADLE_MIN_MAJOR) { [int]$env:GRADLE_MIN_MAJOR } else { 8 }
$installScript = Join-Path $PSScriptRoot 'install-gradle.ps1'
$cacheRoot = if ($env:GRADLE_USER_HOME) {
    $env:GRADLE_USER_HOME
} elseif ($env:XDG_CACHE_HOME) {
    Join-Path $env:XDG_CACHE_HOME 'xenoverse/gradle'
} else {
    Join-Path $env:USERPROFILE '.cache\xenoverse\gradle'
}
$gradleUserHome = $cacheRoot
$env:GRADLE_USER_HOME = $gradleUserHome
$projectCacheDir = if ($env:PROJECT_CACHE_DIR) {
    $env:PROJECT_CACHE_DIR
} else {
    Join-Path $gradleUserHome 'project-cache'
}
$outputBuildDir = if ($env:OUTPUT_BUILD_DIR) {
    $env:OUTPUT_BUILD_DIR
} else {
    Join-Path $gradleUserHome 'build'
}
$distDir = Join-Path $rootDir 'dist'
$jarName = 'xenoverse-portable.jar'

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

$gradleCmd = $null
if (Get-Command gradle -ErrorAction SilentlyContinue) {
    $existing = (Get-Command gradle).Source
    if ((Get-GradleMajorVersion -GradleExe $existing) -ge $minMajor) {
        $gradleCmd = 'gradle'
    }
}

if (-not $gradleCmd) {
    if (Test-Path $installScript) {
        & $installScript -Version $gradleVersion
        $gradleCmd = Join-Path $env:LOCALAPPDATA "Gradle\gradle-$gradleVersion\bin\gradle.bat"
    } elseif (Test-Path -Path '.\gradlew.bat') {
        $gradleCmd = '.\gradlew.bat'
    } else {
        throw 'Gradle is required but was not found on PATH.'
    }
}

New-Item -ItemType Directory -Force -Path $gradleUserHome | Out-Null
New-Item -ItemType Directory -Force -Path $projectCacheDir | Out-Null
New-Item -ItemType Directory -Force -Path $outputBuildDir | Out-Null
New-Item -ItemType Directory -Force -Path $distDir | Out-Null
$publishedJar = Join-Path $distDir $jarName
$checksumFile = "$publishedJar.sha256"
Remove-Item -LiteralPath $publishedJar -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $checksumFile -Force -ErrorAction SilentlyContinue

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java 21 or newer is required but was not found on PATH.'
}

& $gradleCmd --gradle-user-home $gradleUserHome --project-cache-dir $projectCacheDir "-PxenoverseBuildDir=$outputBuildDir" clean portableJar
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$sourceJar = Join-Path $outputBuildDir "libs\$jarName"
if (-not (Test-Path -LiteralPath $sourceJar -PathType Leaf)) {
    throw "Build completed, but the portable JAR was not created at: $sourceJar"
}

Copy-Item -LiteralPath $sourceJar -Destination $publishedJar -Force
$hash = (Get-FileHash -LiteralPath $publishedJar -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath $checksumFile -Value "$hash  $jarName" -Encoding ASCII
Write-Host "Portable game created: $publishedJar"
Write-Host "SHA-256: $hash"
