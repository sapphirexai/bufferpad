param(
    [switch]$Force
)

. (Join-Path $PSScriptRoot 'common.ps1')

$installerHome = Get-InstallerHome
$packageDir = Join-Path $installerHome 'packages'
New-Directory -Path $packageDir

try {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
} catch {
    Write-Warn "Could not force TLS 1.2. Continuing with system defaults."
}

$packages = @(
    @{
        Name = 'JDK 17'
        FileName = 'jdk-17-windows-x64.zip'
        Kind = 'zip'
        Urls = @(
            'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk'
        )
    },
    @{
        Name = 'MySQL 8.0 Windows x64'
        FileName = 'mysql-8.0.46-winx64.zip'
        Kind = 'zip'
        Urls = @(
            'https://dev.mysql.com/get/Downloads/MySQL-8.0/mysql-8.0.46-winx64.zip',
            'https://cdn.mysql.com/Downloads/MySQL-8.0/mysql-8.0.46-winx64.zip'
        )
    },
    @{
        Name = 'nginx Windows'
        FileName = 'nginx-windows.zip'
        Kind = 'zip'
        Urls = @(
            'https://nginx.org/download/nginx-1.26.3.zip',
            'https://nginx.org/download/nginx-1.26.2.zip',
            'https://nginx.org/download/nginx-1.24.0.zip'
        )
    },
    @{
        Name = 'WinSW x64'
        FileName = 'winsw-x64.exe'
        Kind = 'exe'
        Urls = @(
            'https://github.com/winsw/winsw/releases/download/v2.12.0/WinSW-x64.exe'
        )
    },
    @{
        Name = 'Microsoft Visual C++ Redistributable x64'
        FileName = 'VC_redist.x64.exe'
        Kind = 'exe'
        Urls = @(
            'https://aka.ms/vs/17/release/vc_redist.x64.exe'
        )
    }
)

function Invoke-PackageDownload {
    param(
        [hashtable]$Package
    )

    $destination = Join-Path $packageDir $Package.FileName
    if ((Test-Path -LiteralPath $destination) -and -not $Force) {
        Write-Ok "$($Package.Name) already exists: $destination"
        if ($Package.Kind -eq 'zip') {
            Test-ZipFile -Path $destination
        }
        return
    }

    $temp = "$destination.download"
    if ((Test-Path -LiteralPath $temp) -and (Get-Item -LiteralPath $temp).Length -eq 0) {
        Remove-Item -LiteralPath $temp -Force
    }

    $lastError = $null
    $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
    foreach ($url in $Package.Urls) {
        for ($attempt = 1; $attempt -le 3; $attempt++) {
            try {
                Write-Step "Downloading $($Package.Name) (attempt $attempt)"
                Write-Host $url -ForegroundColor DarkGray
                if ($null -ne $curl) {
                    $curlArgs = @('-L', '--fail', '--retry', '3', '--retry-delay', '2', '--connect-timeout', '30', '--max-time', '1800')
                    if (Test-Path -LiteralPath $temp) {
                        $partialSize = (Get-Item -LiteralPath $temp).Length
                        if ($partialSize -gt 0) {
                            Write-Warn "Resuming partial download: $partialSize bytes"
                            $curlArgs += @('-C', '-')
                        }
                    }
                    $curlArgs += @('-o', $temp, $url)
                    & $curl.Source @curlArgs
                    if ($LASTEXITCODE -ne 0) {
                        Fail "curl failed with exit code $LASTEXITCODE"
                    }
                } else {
                    if (Test-Path -LiteralPath $temp) {
                        Remove-Item -LiteralPath $temp -Force
                    }
                    Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $temp -TimeoutSec 1800
                }
                if (-not (Test-Path -LiteralPath $temp) -or (Get-Item -LiteralPath $temp).Length -le 0) {
                    Fail "Downloaded file is empty: $temp"
                }
                Move-Item -LiteralPath $temp -Destination $destination -Force
                if ($Package.Kind -eq 'zip') {
                    Test-ZipFile -Path $destination
                }
                Write-Ok "Downloaded $($Package.Name): $destination"
                return
            } catch {
                $lastError = $_.Exception.Message
                Write-Warn "Download failed: $lastError"
                if ((Test-Path -LiteralPath $temp) -and (Get-Item -LiteralPath $temp).Length -eq 0) {
                    Remove-Item -LiteralPath $temp -Force -ErrorAction SilentlyContinue
                }
                Start-Sleep -Seconds 2
            }
        }
    }

    Fail "Could not download $($Package.Name). Please download it manually to: $destination. Last error: $lastError"
}

foreach ($package in $packages) {
    Invoke-PackageDownload -Package $package
}

Write-Ok "All packages are ready under $packageDir"
