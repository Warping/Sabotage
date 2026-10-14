# Simple Maven Installer for Windows
# Run as Administrator

$MavenVersion = "3.10.0"
$MavenHome = "C:\Maven"
$MavenUrl = "https://dlcdn.apache.org/maven/maven-3/$MavenVersion/binaries/apache-maven-$MavenVersion-bin.zip"
$ZipPath = "$env:TEMP\maven.zip"

Write-Host "Installing Maven $MavenVersion..." -ForegroundColor Cyan

Write-Host "Downloading Maven..." -ForegroundColor Yellow
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
(New-Object System.Net.WebClient).DownloadFile($MavenUrl, $ZipPath)
Write-Host "Downloaded" -ForegroundColor Green

Write-Host "Extracting..." -ForegroundColor Yellow
if (Test-Path $MavenHome) { Remove-Item $MavenHome -Recurse -Force }
Expand-Archive $ZipPath -DestinationPath $env:TEMP
Move-Item "$env:TEMP\apache-maven-$MavenVersion" $MavenHome
Write-Host "Extracted to $MavenHome" -ForegroundColor Green

Write-Host "Adding to system PATH..." -ForegroundColor Yellow
$CurrentPath = [Environment]::GetEnvironmentVariable("Path", "Machine")
$MavenBinPath = "$MavenHome\bin"
if ($CurrentPath -notlike "*$MavenBinPath*") {
    $NewPath = "$CurrentPath;$MavenBinPath"
    [Environment]::SetEnvironmentVariable("Path", $NewPath, "Machine")
    Write-Host "Added to PATH" -ForegroundColor Green
} else {
    Write-Host "Already in PATH" -ForegroundColor Gray
}

Remove-Item $ZipPath -Force
Write-Host ""
Write-Host "Maven installation complete!" -ForegroundColor Green
Write-Host ""
Write-Host "RESTART PowerShell for PATH changes to take effect." -ForegroundColor Yellow
Write-Host ""
Write-Host "Verify with: mvn --version" -ForegroundColor Cyan
