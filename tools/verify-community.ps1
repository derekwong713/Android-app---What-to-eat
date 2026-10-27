param([switch]$Connected, [switch]$Clean, [switch]$Isolated, [switch]$TestApk)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $taskRoot
$taskBundledJdk = Join-Path $taskRoot '.tools\jdk17'
if (Test-Path -LiteralPath $taskBundledJdk) {
    $env:JAVA_HOME = (Get-ChildItem -LiteralPath $taskBundledJdk -Directory | Select-Object -First 1).FullName
}
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to JDK 17 or 21 before running verification.' }
# Reuse Android Studio's configured Gradle cache; isolated outputs avoid IDE file locks.
$taskTemp = Join-Path $taskRoot '.tools\tmp'
New-Item -ItemType Directory -Path $taskTemp -Force | Out-Null
$env:TEMP = $taskTemp
$env:TMP = $taskTemp
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir="' + $taskTemp + '" -Djava.io.tmpdir="' + $taskTemp + '"'
$taskChecks = @(':app:assembleDemoDebug', ':app:assembleLiveDebug', ':app:testDemoDebugUnitTest', ':app:testLiveDebugUnitTest', ':app:lintDemoDebug', ':app:lintLiveDebug')
if ($Clean) { $taskChecks = @(':app:clean') + $taskChecks }
if ($Connected) { $taskChecks += ':app:connectedDemoDebugAndroidTest' }
elseif ($TestApk) { $taskChecks += ':app:assembleDemoDebugAndroidTest' }
$taskOptions = @('--no-daemon', '--continue', '--console=plain')
if ($Isolated) {
    $taskOptions += @('--init-script', (Join-Path $PSScriptRoot 'isolated-verification.gradle'), '--project-cache-dir', (Join-Path $taskRoot '.build-cache\verification-project'))
}
& .\gradlew.bat @taskChecks @taskOptions
exit $LASTEXITCODE
