<#
Simple script to generate Android icons from Tohru_Judge.png
#>

$imagePath = "D:\Kobayashi\Developpement\shield\Tohru_Judge.png"
$outputDir = "D:\Kobayashi\Developpement\shield\app\src\main\res"

# Icon sizes
$sizes = @{
    "mdpi" = 48
    "hdpi" = 72
    "xhdpi" = 96
    "xxhdpi" = 144
    "xxxhdpi" = 192
}

Write-Host "Generating Android icons from Tohru_Judge.png..." -ForegroundColor Cyan

# Load .NET assembly for image processing
Add-Type -AssemblyName System.Drawing

# Load the source image
try {
    $sourceImage = [System.Drawing.Image]::FromFile($imagePath)
    Write-Host "Source image loaded: $($sourceImage.Width)x$($sourceImage.Height)" -ForegroundColor Green
    
    # Generate square icons
    foreach ($key in $sizes.Keys) {
        $size = $sizes[$key]
        $outPath = Join-Path $outputDir "mipmap-$key" "ic_launcher.png"
        
        # Create thumbnail
        $thumbnail = $sourceImage.GetThumbnailImage($size, $size, $null, [IntPtr]::Zero)
        
        # Save as PNG
        $thumbnail.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
        
        Write-Host "Generated: mipmap-$key/ic_launcher.png ($size x $size)" -ForegroundColor Green
        
        $thumbnail.Dispose()
    }
    
    # Generate round icons (simple copy for now - circular mask would require more complex code)
    foreach ($key in $sizes.Keys) {
        $size = $sizes[$key]
        $srcPath = Join-Path $outputDir "mipmap-$key" "ic_launcher.png"
        $outPath = Join-Path $outputDir "mipmap-$key" "ic_launcher_round.png"
        
        # For now, just copy the square icon (circular masking is complex without ImageMagick)
        Copy-Item $srcPath $outPath -Force
        
        Write-Host "Generated: mipmap-$key/ic_launcher_round.png ($size x $size)" -ForegroundColor Green
    }
    
    # Clean up old XML files
    Write-Host ""`n
    Write-Host "Removing old XML icon files..." -ForegroundColor Cyan
    
    Get-ChildItem -Path "$outputDir/mipmap-*" -Filter "ic_launcher*.xml" | Remove-Item -Force -ErrorAction SilentlyContinue
    Get-ChildItem -Path "$outputDir/drawable*" -Filter "ic_launcher.xml" | Remove-Item -Force -ErrorAction SilentlyContinue
    
    Write-Host "Old XML files removed" -ForegroundColor Green
    
    $sourceImage.Dispose()
    
    Write-Host ""`n
    Write-Host "✅ ICONS GENERATED SUCCESSFULLY!" -ForegroundColor Green
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "  1. Rebuild: .\gradlew clean assembleDebug" -ForegroundColor Yellow
    Write-Host "  2. Reinstall: adb install -r app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor Yellow
    
} catch {
    Write-Host "❌ ERROR: $_" -ForegroundColor Red
    exit 1
}
