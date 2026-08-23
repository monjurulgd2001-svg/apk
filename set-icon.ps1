# Run this in PowerShell after saving the logo PNG
# Usage: .\set-icon.ps1 "C:\path\to\logo.png"

param([string]$ImagePath)

if (-not (Test-Path $ImagePath)) {
    Write-Host "Image not found: $ImagePath"
    exit 1
}

$baseDir = "$PSScriptDir\app\src\main\res"

$sizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

Add-Type -AssemblyName System.Drawing

foreach ($folder in $sizes.Keys) {
    $size = $sizes[$folder]
    $outDir = Join-Path $baseDir $folder
    if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

    $outPath = Join-Path $outDir "ic_launcher.png"
    $outPathRound = Join-Path $outDir "ic_launcher_round.png"

    $img = [System.Drawing.Image]::FromFile((Resolve-Path $ImagePath))
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawImage($img, 0, 0, $size, $size)
    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Save($outPathRound, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
    $img.Dispose()

    Write-Host "Created: $outPath ($size x $size)"
}

# Foreground for adaptive icon (432x432)
$fgDir = Join-Path $baseDir "drawable"
if (-not (Test-Path $fgDir)) { New-Item -ItemType Directory -Path $fgDir -Force | Out-Null }
$fgPath = Join-Path $fgDir "ic_launcher_foreground.png"
$fgSize = 432
$img = [System.Drawing.Image]::FromFile((Resolve-Path $ImagePath))
$bmp = New-Object System.Drawing.Bitmap($fgSize, $fgSize)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.DrawImage($img, 0, 0, $fgSize, $fgSize)
$bmp.Save($fgPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
$img.Dispose()
Write-Host "Created: $fgPath ($fgSize x $fgSize)"

Write-Host "`nDone! All icons generated."
