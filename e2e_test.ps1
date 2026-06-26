param([string]$Device = "37220DLJG001ML")

$script:PassCount = 0
$script:FailCount = 0

function Test-Search {
    param(
        [string]$Name,
        [string]$Query,
        [string]$Expect,
        [string]$Action = "default"
    )

    Write-Host -NoNewline "  [$Name] `"$Query`" ... "

    $null = adb -s $Device logcat -c 2>$null
    $broadcast = "am broadcast -a com.aria.assistant.TEST_SEARCH -n com.aria.assistant/.skill.TestReceiver --es query '$Query' --es action '$Action'"
    $null = adb -s $Device shell $broadcast 2>$null
    Start-Sleep -Seconds 8

    $allLines = adb -s $Device logcat -d -s Aria:* 2>$null
    $warnLines = @($allLines | Where-Object { $_ -match " W Aria" })

    $matched = @($warnLines | Where-Object { $_ -match $Expect })
    if ($matched.Count -gt 0) {
        Write-Host "PASS" -ForegroundColor Green
        $script:PassCount++
        $highlighted = @($warnLines | Where-Object { $_ -match "DDG API|Weather|Wikipedia|DDG HTML|TEST|Wiki" })
        $highlighted | ForEach-Object {
            Write-Host "      $_"
        }
    } else {
        Write-Host "FAIL (expected: $Expect)" -ForegroundColor Red
        $script:FailCount++
        $warnLines | Select-Object -Last 10 | ForEach-Object {
            Write-Host "      $_"
        }
    }
}

Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Aria E2E On-Device Test Suite" -ForegroundColor Cyan
Write-Host " Device: $Device" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "--- Weather Tests ---" -ForegroundColor Yellow
Test-Search -Name "Weather (London)" -Query "London" -Expect "weather|Weather|°C|°F" -Action "weather"
Test-Search -Name "Weather (Tokyo)" -Query "Tokyo" -Expect "weather|Weather|°C|°F" -Action "weather"
Test-Search -Name "Weather (New York)" -Query "New York" -Expect "weather|Weather|°C|°F" -Action "weather"

Write-Host ""
Write-Host "--- IP Lookup ---" -ForegroundColor Yellow
Test-Search -Name "IP Lookup" -Query "my ip" -Expect "ip|IP|address"

Write-Host ""
Write-Host "--- DDG API Tests ---" -ForegroundColor Yellow
Test-Search -Name "DDG API (who is president)" -Query "who is president of USA" -Expect "DDG API"
Test-Search -Name "DDG API (quantum computing)" -Query "quantum computing" -Expect "DDG API|Wikipedia|abstract"

Write-Host ""
Write-Host "--- DDG HTML Tests ---" -ForegroundColor Yellow
Test-Search -Name "DDG HTML (things to do)" -Query "fun things to do in London" -Expect "result__a|DDG HTML"
Test-Search -Name "DDG HTML (weather links)" -Query "weather forecast London" -Expect "result__a|DDG HTML|Weather"

Write-Host ""
Write-Host "--- Wikipedia Tests ---" -ForegroundColor Yellow
Test-Search -Name "Wikipedia (quantum)" -Query "quantum computing" -Expect "Wiki|Wikipedia|Quantum"
Test-Search -Name "Wikipedia (president)" -Query "president of France" -Expect "Wiki|Wikipedia|President|France"

Write-Host ""
Write-Host "--- Location Extraction ---" -ForegroundColor Yellow
Test-Search -Name "Location (weather in X)" -Query "weather in Tokyo Japan" -Expect "Weather|°C|°F|Japan" -Action "weather"
Test-Search -Name "Location (forecast X)" -Query "forecast for Paris" -Expect "Weather|°C|°F" -Action "weather"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Results: $PassCount passed, $FailCount failed" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

exit $(if ($FailCount -gt 0) { 1 } else { 0 })
