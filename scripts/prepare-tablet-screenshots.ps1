$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$source = Join-Path $root 'play-store/graphics/phone-screenshots/framed'
$sets = @(
    @{ Directory = 'seven-inch-screenshots/framed'; Width = 1200; Height = 1920 },
    @{ Directory = 'ten-inch-screenshots/framed'; Width = 1600; Height = 2560 }
)

$screenshots = Get-ChildItem -LiteralPath $source -File | Where-Object { $_.Extension -match '^\.(png|jpe?g)$' } | Sort-Object Name
if ($screenshots.Count -lt 2) { throw 'At least two framed phone screenshots are required' }

foreach ($set in $sets) {
    $outputDirectory = Join-Path (Join-Path $root 'play-store/graphics') $set.Directory
    New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
    Get-ChildItem -LiteralPath $outputDirectory -File -ErrorAction SilentlyContinue | Remove-Item -Force

    foreach ($screenshot in $screenshots) {
        $outputPath = Join-Path $outputDirectory $screenshot.Name
        $filter = "scale=$($set.Width):$($set.Height):force_original_aspect_ratio=decrease,pad=$($set.Width):$($set.Height):(ow-iw)/2:(oh-ih)/2:color=0x080b1d"
        & ffmpeg -hide_banner -loglevel error -y -i $screenshot.FullName -vf $filter -frames:v 1 $outputPath
        if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed for $($screenshot.FullName)" }
    }
}
