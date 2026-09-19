# Builds the app and archives the APK under /apks with a versioned, self-describing name.
#
#   pwsh -File tools/save-apk.ps1 -Version 0.3.0 -Slug "gps-fix"
#   pwsh -File tools/save-apk.ps1                      # just archive the current artifact
#
# Options:
#   -Version   semver to write into build.gradle.kts (also bumps versionCode by 1)
#   -Slug      short kebab-case description of the change set
#   -NoBuild   skip gradle and archive whatever is already built

param(
    [string]$Version,
    [string]$Slug = "build",
    [switch]$NoBuild
)

$ErrorActionPreference = "Stop"

$root    = Split-Path -Parent $PSScriptRoot
$android = Join-Path $root "android"
$gradle  = Join-Path $root "android\gradlew.bat"
$apkDir  = Join-Path $android "app\build\outputs\apk\debug"
$apk     = Join-Path $apkDir "app-debug.apk"
$outDir  = Join-Path $root "apks"
$gradleProps = Join-Path $android "app\build.gradle.kts"

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

if ($Version) {
    $text = Get-Content $gradleProps -Raw
    $code = [int]([regex]::Match($text, 'versionCode\s*=\s*(\d+)').Groups[1].Value)
    $text = $text -replace 'versionCode\s*=\s*\d+', "versionCode = $($code + 1)"
    $text = $text -replace 'versionName\s*=\s*"[^"]*"', "versionName = `"$Version`""
    Set-Content $gradleProps -Value $text -Encoding utf8 -NoNewline
    Write-Host "version -> $Version (versionCode $($code + 1))"
}

if (-not $NoBuild) {
    if (Test-Path $gradle) {
        & $gradle -p $android assembleDebug --console=plain --offline
    } else {
        # The temp-directory Gradle gets pruned by Windows cleanup, which silently breaks
        # every build. The copy under the user profile survives; prefer it.
        $fallback = "C:\Users\jagad\gradle-dist\gradle-8.13\bin\gradle.bat"
        if (-not (Test-Path $fallback)) {
            $fallback = "C:\Users\jagad\AppData\Local\Temp\opencode\gradle\gradle-8.13\bin\gradle.bat"
        }
        if (-not (Test-Path $fallback)) { throw "No gradlew and no fallback Gradle found." }
        & $fallback -p $android assembleDebug --console=plain --offline
    }
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed ($LASTEXITCODE)" }
}

if (-not (Test-Path $apk)) { throw "APK not found at $apk" }

$name = Get-Content $gradleProps -Raw | Select-String 'versionName\s*=\s*"([^"]*)"' |
        ForEach-Object { $_.Matches[0].Groups[1].Value } | Select-Object -First 1
if (-not $name) { $name = "0.0.0" }

$stamp   = Get-Date -Format "yyyyMMdd-HHmm"
$target  = Join-Path $outDir "poshan-v$name-$Slug-$stamp.apk"
Copy-Item $apk $target -Force

$size = [int]((Get-Item $target).Length / 1MB)
Write-Host ""
Write-Host "Archived: apks\$(Split-Path -Leaf $target)  ($size MB)"
Get-ChildItem $outDir -Filter *.apk | Sort-Object LastWriteTime |
    Select-Object Name, @{N = 'MB'; E = { [int]($_.Length / 1MB) } }, LastWriteTime |
    Format-Table -AutoSize
