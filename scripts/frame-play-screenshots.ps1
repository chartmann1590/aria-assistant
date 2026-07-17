$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$raw = Join-Path $root 'play-store/graphics/phone-screenshots/raw'
$out = Join-Path $root 'play-store/graphics/phone-screenshots/framed'
$font = 'C\:/Windows/Fonts/segoeuib.ttf'
New-Item -ItemType Directory -Force -Path $out | Out-Null

$screens = @(
    @{ Source = '01-onboarding-language.png'; Output = '01-private-by-design.png'; Caption = 'Private intelligence, right on your phone' },
    @{ Source = '08-main.png'; Output = '02-ask-naturally.png'; Caption = 'Ask naturally. Get things done.' },
    @{ Source = '10-web-result.png'; Output = '03-web-verification.png'; Caption = 'Verify answers across the web' },
    @{ Source = '09-settings.png'; Output = '04-on-device-translation.png'; Caption = 'Translate menus on device' },
    @{ Source = '06-permissions-choice.png'; Output = '05-permission-control.png'; Caption = 'You control every permission' },
    @{ Source = '11-feedback.png'; Output = '06-quality-feedback.png'; Caption = 'Private feedback, built in' }
)

foreach ($screen in $screens) {
    $inputPath = Join-Path $raw $screen.Source
    if (-not (Test-Path $inputPath)) { throw "Missing screenshot: $inputPath" }
    $outputPath = Join-Path $out $screen.Output
    $caption = $screen.Caption.Replace("'", "\\'").Replace(':', '\:')
    $filter = "[0:v]drawbox=x=0:y=0:w=iw:h=280:color=0x321b5f:t=fill,drawbox=x=174:y=294:w=732:h=1582:color=black@0.55:t=fill,drawtext=fontfile='$font':text='$caption':fontcolor=white:fontsize=48:x=(w-text_w)/2:y=105[bg];[1:v]scale=700:-2[phone];[bg][phone]overlay=(W-w)/2:306,drawbox=x=184:y=300:w=712:h=1570:color=0x8b5cf6@0.8:t=6"
    & ffmpeg -hide_banner -loglevel error -y -f lavfi -i 'color=c=0x080b1d:s=1080x1920' -i $inputPath -filter_complex $filter -frames:v 1 $outputPath
    if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed for $($screen.Source)" }
}
