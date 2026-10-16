$body = '{"username":"admin","password":"admin123"}'
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginRes = Invoke-RestMethod -Uri 'http://localhost:8088/api/auth/login' -Method Post -ContentType 'application/json' -Body $body -WebSession $session
Write-Output "=== TEST 1: AUTHENTICATION ==="
Write-Output "Login success! Username: $($loginRes.username), DisplayName: $($loginRes.displayName), Role: $($loginRes.role)"

Write-Output "`n=== TEST 2: WRITE TO NEON DATABASE ==="
$classBody = [System.Text.Encoding]::UTF8.GetBytes('{"classCode":"NET-CLOUD-01","name":"Lop Lap Trinh Mang Cloud","description":"Cloud Computing Course"}')
$classRes = Invoke-RestMethod -Uri 'http://localhost:8088/api/classes' -Method Post -ContentType 'application/json; charset=utf-8' -Body $classBody -WebSession $session
Write-Output "Created Class on Neon DB! ID: $($classRes.id), Name: $($classRes.name), Code: $($classRes.classCode)"

Write-Output "`n=== TEST 3: READ FROM NEON DATABASE ==="
$allClasses = Invoke-RestMethod -Uri 'http://localhost:8088/api/classes' -Method Get -WebSession $session
Write-Output "Total Classes queried from Neon DB: $($allClasses.Count)"
foreach ($c in $allClasses) {
    Write-Output " - [$($c.code)] $($c.name) (Subject: $($c.subject))"
}
