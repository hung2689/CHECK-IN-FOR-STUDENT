# ==============================================================================
# Script Test Tai Dong Thoi: 70 Sinh Vien Check-in Cung Luc
# ==============================================================================
param(
    [string]$BackendUrl = "",
    [string]$CheckinLink = ""
)

Add-Type -AssemblyName System.Net.Http

# Mo rong gioi han ket noi dong thoi tren Windows PowerShell (Mac dinh .NET chi cho 2 ket noi cung luc!)
[System.Net.ServicePointManager]::DefaultConnectionLimit = 200

# Danh sach 70 MSSV chinh xac cua lop
$studentCodes = @(
    "DS220147", "DS220154", "DS220161", "DS220189", "DS220210",
    "DS220226", "DS220296", "DS220310", "DS220312", "DS220338",
    "DS220142", "DS220143", "DS220146", "DS220153", "DS220166",
    "DS220168", "DS220170", "DS220179", "DS220182", "DS220185",
    "DS220192", "DS220202", "DS220206", "DS220208", "DS220213",
    "DS220214", "DS220220", "DS220221", "DS220222", "DS220224",
    "DS220237", "DS220247", "DS220253", "DS220270", "DS220284",
    "DS220288", "DS220289", "DS220290", "DS220291", "DS220293",
    "DS220299", "DS220307", "DS220315", "DS220316", "DS220319",
    "DS220329", "DS220332", "DS220334", "DS220335", "DS220340",
    "DS220343", "DS220348", "DS220349", "DS220350", "DS220351",
    "DS220367", "DS220386", "DS220388", "DS220394", "DS220406",
    "DS220408", "DS220414", "DS220416", "DS220428", "DS220433",
    "DS220436", "DS224003", "DS224010", "DS224512", "DS224550"
)

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  TEST TAI DONG THOI: 70 SINH VIEN CHECK-IN CUNG LUC" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Nhap Backend URL neu chua co
if ([string]::IsNullOrWhiteSpace($BackendUrl)) {
    $BackendUrl = Read-Host "1. Nhap URL Backend tren Render (VD: https://check-in-xxxx.onrender.com)"
}
$BackendUrl = $BackendUrl.Trim().TrimEnd('/')

# 2. Nhap link diem danh (hoac Token)
if ([string]::IsNullOrWhiteSpace($CheckinLink)) {
    $CheckinLink = Read-Host "2. Bam nut 'Sao chep link diem danh' tren web roi Paste vao day"
}
$CheckinLink = $CheckinLink.Trim()

# Trich xuat token
$token = $CheckinLink
if ($token -match '/checkin/([^/?#]+)') {
    $token = $Matches[1]
} elseif ($token.Contains('/')) {
    $token = $token.Split('/')[-1]
}

Write-Host "`n[*] Dang kiem tra trang thai phong diem danh..." -ForegroundColor Yellow
try {
    $tokenInfo = Invoke-RestMethod -Uri "$BackendUrl/api/public/attendance/$token" -Method Get -TimeoutSec 15
    if (-not $tokenInfo.valid) {
        Write-Host "LOI: $($tokenInfo.message)" -ForegroundColor Red
        Write-Host "Hay chac chan phong diem danh dang o trang thai Mo (Chua bi Chot check-in)!" -ForegroundColor Yellow
        exit 1
    }
    Write-Host " -> Hop le! Lop: $($tokenInfo.className) | Buoi: $($tokenInfo.sessionName) | Giai doan: $($tokenInfo.stage)" -ForegroundColor Green
} catch {
    Write-Host "LOI ket noi den Backend: $_" -ForegroundColor Red
    exit 1
}

Write-Host "`n[*] CHUAN BI BAN DONG THOI 70 REQUESTS CUNG 1 LUC..." -ForegroundColor Cyan
Write-Host "==> Hay quan sat man hinh web: cac sinh vien se nhay xanh Check-in!" -ForegroundColor Magenta
Write-Host ""

$endpoint = "$BackendUrl/api/public/attendance/$token"
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(60)

# Tao va kich hoat 70 requests dong thoi
$threads = $studentCodes | ForEach-Object {
    $code = $_
    $json = "{`"studentCode`":`"$code`"}"
    $content = New-Object System.Net.Http.StringContent($json, [System.Text.Encoding]::UTF8, "application/json")
    
    $reqStopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $responseTask = $client.PostAsync($endpoint, $content)
    
    [PSCustomObject]@{
        Code = $code
        Task = $responseTask
        Timer = $reqStopwatch
    }
}

Write-Host "Da phong het 70 requests len server! Dang nhan ket qua..." -ForegroundColor Yellow

$successCount = 0
$failCount = 0
$latencies = New-Object System.Collections.Generic.List[double]

foreach ($item in $threads) {
    try {
        $response = $item.Task.Result
        $item.Timer.Stop()
        $durationMs = $item.Timer.ElapsedMilliseconds
        $latencies.Add($durationMs)

        if ($response.IsSuccessStatusCode) {
            $successCount++
            Write-Host " [OK 200] $($item.Code) - ${durationMs}ms" -ForegroundColor Green
        } else {
            $failCount++
            $status = [int]$response.StatusCode
            Write-Host " [FAIL $status] $($item.Code) - ${durationMs}ms" -ForegroundColor Red
        }
    } catch {
        $item.Timer.Stop()
        $failCount++
        Write-Host " [ERR] $($item.Code) - $($_.Exception.Message)" -ForegroundColor Red
    }
}

$stopwatch.Stop()
$client.Dispose()

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "               KET QUA TEST TAI 70 SINH VIEN              " -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Tong thoi gian xu ly : $($stopwatch.ElapsedMilliseconds) ms (~$([Math]::Round($stopwatch.Elapsed.TotalSeconds, 2)) giay)" -ForegroundColor White
Write-Host " Thanh cong           : $successCount / 70 ($([Math]::Round(($successCount / 70) * 100, 1))%)" -ForegroundColor Green
if ($failCount -gt 0) {
    Write-Host " That bai / Loi       : $failCount" -ForegroundColor Red
}
if ($latencies.Count -gt 0) {
    $avg = [Math]::Round(($latencies | Measure-Object -Average).Average, 1)
    $min = ($latencies | Measure-Object -Minimum).Minimum
    $max = ($latencies | Measure-Object -Maximum).Maximum
    Write-Host " Phan hoi trung binh  : $avg ms / sinh vien" -ForegroundColor Cyan
    Write-Host " Phan hoi nhanh nhat  : $min ms" -ForegroundColor Cyan
    Write-Host " Phan hoi cham nhat   : $max ms" -ForegroundColor Cyan
}
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Nhin sang man hinh web TA: xem 70 sinh vien da duoc diem danh!" -ForegroundColor Green
