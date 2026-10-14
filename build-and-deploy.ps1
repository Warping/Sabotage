# Build, Deploy, and Reset Sabotage Plugin Script
# Usage: .\build-and-deploy.ps1 [build|clean|reset]

param(
    [ValidateSet('build', 'clean', 'reset')]
    [string]$Mode = 'build'
)

$SabotageDir = "C:\Users\ZAKSGAMINGRIG\Desktop\Sabotage"
$ServerDir = "C:\Users\ZAKSGAMINGRIG\Desktop\test plugin server"
$PluginDir = "$ServerDir\plugins"
$ConfigDir = "$ServerDir\plugins\Sabotage"
$PluginJar = "sabotage.plugin-1.0-SNAPSHOT.jar"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Sabotage Plugin Build and Deploy Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Mode: Build and Deploy
if ($Mode -eq 'build') {
    Write-Host "[1/3] Building plugin with Maven..." -ForegroundColor Yellow
    Push-Location $SabotageDir
    mvn clean package -q
    $BuildExitCode = $LASTEXITCODE
    Pop-Location
    if ($BuildExitCode -ne 0) {
        Write-Host "[FAIL] Build failed!" -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] Build successful!" -ForegroundColor Green

    Write-Host "[2/3] Plugin copied to server/plugins (automatic via maven-jar-plugin)" -ForegroundColor Green

    Write-Host "[3/3] Verifying deployment..." -ForegroundColor Yellow
    if (Test-Path "$PluginDir\$PluginJar") {
        $FileInfo = Get-Item "$PluginDir\$PluginJar"
        Write-Host "[OK] Deployed: $PluginJar" -ForegroundColor Green
        Write-Host "  Location: $PluginDir" -ForegroundColor Gray
        Write-Host "  Size: $([math]::Round($FileInfo.Length / 1KB, 1)) KB" -ForegroundColor Gray
        Write-Host "  Time: $($FileInfo.LastWriteTime)" -ForegroundColor Gray
    } else {
        Write-Host "[FAIL] Plugin not found in plugins directory!" -ForegroundColor Red
        exit 1
    }

    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Build Complete! Ready to run server." -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "To start the server, run:" -ForegroundColor Cyan
    Write-Host "  cd '$ServerDir'" -ForegroundColor White
    Write-Host "  java -Xmx1024M -Xms1024M -jar paper-26.2-129.jar nogui" -ForegroundColor White
}

# Mode: Clean (remove built plugin)
elseif ($Mode -eq 'clean') {
    Write-Host "Cleaning plugin from server..." -ForegroundColor Yellow
    if (Test-Path "$PluginDir\$PluginJar") {
        Remove-Item "$PluginDir\$PluginJar" -Force
        Write-Host "[OK] Plugin removed" -ForegroundColor Green
    } else {
        Write-Host "[SKIP] Plugin not found (nothing to clean)" -ForegroundColor Gray
    }
    if (Test-Path "$ConfigDir") {
        Remove-Item "$ConfigDir" -Recurse -Force
        Write-Host "[OK] Configuration removed" -ForegroundColor Green
    } else {
        Write-Host "[SKIP] Configuration not found (nothing to clean)" -ForegroundColor Gray
    }
}

# Mode: Reset (clean + rebuild)
elseif ($Mode -eq 'reset') {
    Write-Host "Performing FULL RESET..." -ForegroundColor Yellow
    Write-Host ""

    # Remove the plugin
    if (Test-Path "$PluginDir\$PluginJar") {
        Remove-Item "$PluginDir\$PluginJar" -Force
        Write-Host "[OK] Old plugin removed" -ForegroundColor Green
    }
    
    # Remove the configuration
    if (Test-Path "$ConfigDir") {
        Remove-Item "$ConfigDir" -Recurse -Force
        Write-Host "[OK] Old configuration removed" -ForegroundColor Green
    } else {
        Write-Host "[SKIP] Old configuration not found (nothing to remove)" -ForegroundColor Gray
    }

    # Clean Maven cache
    Write-Host "Cleaning Maven build artifacts..." -ForegroundColor Yellow
    Push-Location $SabotageDir
    mvn clean -q
    $CleanExitCode = $LASTEXITCODE
    if ($CleanExitCode -eq 0) {
        Write-Host "[OK] Maven artifacts cleaned" -ForegroundColor Green
    } else {
        Write-Host "[WARN] Maven clean had warnings (continuing...)" -ForegroundColor Yellow
    }

    # Fresh build
    Write-Host "Building fresh plugin..." -ForegroundColor Yellow
    mvn package -q
    $PackageExitCode = $LASTEXITCODE
    Pop-Location
    if ($PackageExitCode -ne 0) {
        Write-Host "[FAIL] Build failed!" -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] Fresh build successful!" -ForegroundColor Green

    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "RESET Complete!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
}

Write-Host ""
