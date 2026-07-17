$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$videoDir = Join-Path $root 'play-store/graphics/promo-video'
$framesDir = Join-Path $videoDir 'frames'
$font = 'C\:/Windows/Fonts/segoeuib.ttf'
New-Item -ItemType Directory -Force -Path $framesDir | Out-Null

$narration = @'
Meet Aria, the private assistant designed for your Android phone. Choose your language from the start, then ask naturally by voice or text. Aria can verify current information across the public web and show the sources behind an answer. Translation models download on demand and continue working on device. Every sensitive permission stays under your control, and private feedback is built in. Aria brings capable, transparent assistance to your pocket.
'@
$audio = Join-Path $videoDir 'aria-promo-narration.mp3'
& edge-tts --voice 'en-US-AriaNeural' '--rate=-5%' --text $narration.Trim() --write-media $audio
if ($LASTEXITCODE -ne 0) { throw 'Narration generation failed' }

$slides = @(
    @{ Path = 'play-store/graphics/feature-graphic/aria-feature-graphic-1024x500.png'; Seconds = 5; Intro = $true },
    @{ Path = 'play-store/graphics/phone-screenshots/framed/01-private-by-design.png'; Seconds = 5 },
    @{ Path = 'play-store/graphics/phone-screenshots/framed/02-ask-naturally.png'; Seconds = 5 },
    @{ Path = 'play-store/graphics/phone-screenshots/framed/03-web-verification.png'; Seconds = 6 },
    @{ Path = 'play-store/graphics/phone-screenshots/framed/04-on-device-translation.png'; Seconds = 5 },
    @{ Path = 'play-store/graphics/phone-screenshots/framed/05-permission-control.png'; Seconds = 5 },
    @{ Path = 'play-store/graphics/phone-screenshots/framed/06-quality-feedback.png'; Seconds = 5 }
)

$concatLines = @()
for ($i = 0; $i -lt $slides.Count; $i++) {
    $slide = $slides[$i]
    $input = Join-Path $root $slide.Path
    $frame = Join-Path $framesDir ('slide-{0:d2}.png' -f $i)
    if ($slide.Intro) {
        $filter = "scale=1920:1080:force_original_aspect_ratio=increase,crop=1920:1080,drawbox=x=0:y=0:w=950:h=1080:color=0x080b1d@0.78:t=fill,drawtext=fontfile='$font':text='ARIA':fontcolor=white:fontsize=150:x=145:y=300,drawtext=fontfile='$font':text='Private intelligence. Global reach.':fontcolor=0xbca7ff:fontsize=48:x=150:y=490"
    } else {
        $filter = "split=2[base][front];[base]scale=1920:1080:force_original_aspect_ratio=increase,crop=1920:1080,boxblur=30:10,colorchannelmixer=aa=0.35[bg];[front]scale=-2:1020[fg];[bg][fg]overlay=(W-w)/2:(H-h)/2"
    }
    & ffmpeg -hide_banner -loglevel error -y -i $input -vf $filter -frames:v 1 $frame
    if ($LASTEXITCODE -ne 0) { throw "Slide rendering failed: $input" }
    $normalized = $frame.Replace('\', '/')
    $concatLines += "file '$normalized'"
    $concatLines += "duration $($slide.Seconds)"
}
$concatLines += $concatLines[-2]
$concatFile = Join-Path $videoDir 'slides.txt'
[System.IO.File]::WriteAllLines($concatFile, $concatLines, [System.Text.UTF8Encoding]::new($false))

$output = Join-Path $videoDir 'aria-google-play-promo-1080p.mp4'
& ffmpeg -hide_banner -loglevel error -y -f concat -safe 0 -i $concatFile -i $audio -c:v libx264 -preset medium -crf 18 -r 30 -pix_fmt yuv420p -c:a aac -b:a 192k -shortest -movflags +faststart $output
if ($LASTEXITCODE -ne 0) { throw 'Promo video encoding failed' }
Write-Output $output
