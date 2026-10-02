# Laptop / Windows PowerShell. Fetches the pinned official Gradle wrapper.
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$Root = Split-Path -Parent $PSScriptRoot
$Upstream = 'https://raw.githubusercontent.com/gradle/gradle/v8.9.0'
New-Item -ItemType Directory -Force (Join-Path $Root 'gradle/wrapper') | Out-Null
foreach ($File in @('gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar')) {
    Invoke-WebRequest -UseBasicParsing "$Upstream/$File" -OutFile (Join-Path $Root $File)
}
$Checksum = Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-8.9-wrapper.jar.sha256'
$Expected = if ($Checksum.Content -is [byte[]]) {
    [Text.Encoding]::ASCII.GetString($Checksum.Content).Trim()
} else { ([string]$Checksum.Content).Trim() }
$Actual = (Get-FileHash (Join-Path $Root 'gradle/wrapper/gradle-wrapper.jar') -Algorithm SHA256).Hash.ToLowerInvariant()
if ($Actual -ne $Expected.ToLowerInvariant()) { throw 'Wrapper checksum mismatch; do not run Gradle.' }
Write-Host 'Official Gradle 8.9 wrapper installed and checksum checked. Open the project in Android Studio.'
