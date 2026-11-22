$url = "http://localhost:9090/api/users"

function Send-Burst($headers, $count) {
    $ok = 0; $blocked = 0; $denied = 0; $other = 0
    for ($i = 1; $i -le $count; $i++) {
        $remaining = ""
        try {
            $r = Invoke-WebRequest -Uri $url -Headers $headers -UseBasicParsing
            $code = [int]$r.StatusCode
            $remaining = "$($r.Headers['X-RateLimit-Remaining'])"
        } catch {
            if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode } else { $code = 0 }
        }
        switch ($code) {
            200 { $ok++;      Write-Host ("Request {0,3} : 200 OK (allowed)        remaining={1}" -f $i, $remaining) -ForegroundColor Green }
            429 { $blocked++; Write-Host ("Request {0,3} : 429 BLOCKED (rate limit exceeded)" -f $i) -ForegroundColor Red }
            401 { $denied++;  Write-Host ("Request {0,3} : 401 REJECTED (bad or missing key)" -f $i) -ForegroundColor Yellow }
            default { $other++; Write-Host ("Request {0,3} : {1} (unexpected - is the gateway running?)" -f $i, $code) -ForegroundColor Magenta }
        }
    }
    Write-Host ""
    Write-Host "Summary: $ok allowed, $blocked blocked (429), $denied rejected (401), $other other"
}

Write-Host ""
Write-Host "Gateway attack demo   target: $url"
Write-Host "  1 = Flood with a VALID key  (shows 429 rate limiting)"
Write-Host "  2 = No API key              (shows 401)"
Write-Host "  3 = Wrong API key           (shows 401)"
Write-Host ""
$choice = Read-Host "Choose 1, 2 or 3"
$countText = Read-Host "How many requests? (press Enter for 15)"
$count = 15
if ($countText -match '^\d+$' -and [int]$countText -gt 0) { $count = [int]$countText }
Write-Host ""

switch ($choice) {
    "1" { $key = Read-Host "Paste your API key"; Write-Host ""; Send-Burst @{ "X-API-Key" = $key.Trim() } $count }
    "2" { Send-Burst @{} $count }
    "3" { Send-Burst @{ "X-API-Key" = "rl_thisIsNotARealKey" } $count }
    default { Write-Host "Please run it again and choose 1, 2 or 3." }
}
