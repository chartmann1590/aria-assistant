#!/bin/bash
# E2E on-device test script for Aria
# Usage: bash e2e_test.sh [device_serial]

DEV=${1:-37220DLJG001ML}
PASS=0
FAIL=0

red() { echo -e "\033[31m$1\033[0m"; }
green() { echo -e "\033[32m$1\033[0m"; }
yellow() { echo -e "\033[33m$1\033[0m"; }

test_search() {
    local name="$1"
    local query="$2"
    local expect="$3"
    local action="${4:-default}"
    
    echo -n "  [$name] \"$query\" ... "
    
    adb -s "$DEV" logcat -c 2>/dev/null
    adb -s "$DEV" shell "am broadcast -a com.aria.assistant.TEST_SEARCH -n com.aria.assistant/.skill.TestReceiver --es query '$query' --es action '$action'" 2>/dev/null >/dev/null
    sleep 8
    
    local output=$(adb -s "$DEV" logcat -d -s Aria:* 2>/dev/null | grep "W Aria")
    
    if echo "$output" | grep -qi "$expect"; then
        green "PASS"
        PASS=$((PASS + 1))
        echo "$output" | grep -E "DDG API|Weather|Wikipedia|DDG HTML|TEST" | while read line; do
            echo "      $line"
        done
    else
        red "FAIL (expected: $expect)"
        FAIL=$((FAIL + 1))
        echo "$output" | tail -10 | while read line; do
            echo "      $line"
        done
    fi
}

echo "============================================"
echo " Aria E2E On-Device Test Suite"
echo " Device: $DEV"
echo "============================================"
echo ""

# Ensure app is running
adb -s "$DEV" shell am start -n com.aria.assistant/.presentation.MainActivity 2>/dev/null
sleep 3

echo "--- Weather Tests ---"
test_search "Weather (London)" "London" "weather|Weather|°C|°F" "weather"
test_search "Weather (Tokyo)" "Tokyo" "weather|Weather|°C|°F" "weather"
test_search "Weather (New York)" "New York" "weather|Weather|°C|°F" "weather"

echo ""
echo "--- IP Lookup ---"
test_search "IP Lookup" "my ip" "ip|IP|address" ""

echo ""
echo "--- DDG API Tests ---"
test_search "DDG API (who is president)" "who is president of USA" "DDG API" ""
test_search "DDG API (quantum computing)" "quantum computing" "DDG API|Wikipedia|abstract" ""

echo ""
echo "--- DDG HTML Tests ---"
test_search "DDG HTML (things to do)" "fun things to do in London" "result__a|DDG HTML" ""
test_search "DDG HTML (weather links)" "weather forecast London" "result__a|DDG HTML|Weather" ""

echo ""
echo "--- Wikipedia Tests ---"
test_search "Wikipedia (quantum)" "quantum computing" "Wiki|Wikipedia|Quantum" ""
test_search "Wikipedia (president)" "president of France" "Wiki|Wikipedia|President|France" ""

echo ""
echo "--- Location Extraction ---"
test_search "Location (weather in X)" "weather in Tokyo Japan" "Weather|°C|°F|Japan" "weather"
test_search "Location (forecast X)" "forecast for Paris" "Weather|°C|°F" "weather"

echo ""
echo "============================================"
echo " Results: $PASS passed, $FAIL failed"
echo "============================================"
