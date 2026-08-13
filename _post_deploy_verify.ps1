$ErrorActionPreference = 'Stop'
$Base = 'https://huashi.mnari.cn'
$OutDir = 'qa-output/e2e-2026-08-06/api-postdeploy'
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

function Login([string]$user, [string]$pass) {
  $body = @{ usernameOrEmail = $user; password = $pass } | ConvertTo-Json
  $resp = Invoke-RestMethod -Method Post -Uri "$Base/api/auth/login" -ContentType 'application/json' -Body $body
  return $resp.data.accessToken
}

function Api([string]$method, [string]$path, [string]$token, $bodyObj = $null, [hashtable]$query = $null) {
  $headers = @{ Authorization = "Bearer $token" }
  $uri = "$Base$path"
  if ($query) {
    $qs = ($query.GetEnumerator() | ForEach-Object { "$($_.Key)=$([uri]::EscapeDataString([string]$_.Value))" }) -join '&'
    $uri = "$uri`?$qs"
  }
  $params = @{ Method = $method; Uri = $uri; Headers = $headers }
  if ($null -ne $bodyObj) {
    $params.ContentType = 'application/json'
    $params.Body = ($bodyObj | ConvertTo-Json -Depth 8 -Compress)
  }
  try {
    $resp = Invoke-WebRequest @params -UseBasicParsing
    return @{ status = [int]$resp.StatusCode; body = $resp.Content }
  } catch {
    $r = $_.Exception.Response
    if ($null -eq $r) { throw }
    $reader = New-Object System.IO.StreamReader($r.GetResponseStream())
    $content = $reader.ReadToEnd()
    return @{ status = [int]$r.StatusCode; body = $content }
  }
}

$results = @()
function Check([string]$name, [bool]$ok, [string]$detail) {
  $line = if ($ok) { "PASS $name :: $detail" } else { "FAIL $name :: $detail" }
  Write-Host $line
  $script:results += [pscustomobject]@{ name = $name; ok = $ok; detail = $detail }
}

Write-Host '== login =='
$admin = Login 'admin' 'Admin@123456'
$teacher = Login 'teacher.zhang' 'Teacher@123456'
$student = Login 'student.li' 'Student@123456'
Check 'login-roles' ($admin -and $teacher -and $student) 'admin/teacher/student tokens issued'

Write-Host '== DEF-001 invalid purpose =='
$r = Api 'GET' '/api/teacher/assessments/papers' $teacher $null @{ purpose = 'RESEARCH' }
$j = $r.body | ConvertFrom-Json
Check 'DEF-001-purpose' (($r.status -eq 400) -and ($j.code -eq 'VALIDATION_ERROR')) "status=$($r.status) code=$($j.code)"

Write-Host '== DEF-001 bad JSON =='
try {
  $headers = @{ Authorization = "Bearer $teacher" }
  $resp = Invoke-WebRequest -Method Post -Uri "$Base/api/teacher/assessments/papers" -Headers $headers -ContentType 'application/json' -Body '{not-json' -UseBasicParsing
  Check 'DEF-001-json' $false "unexpected status=$([int]$resp.StatusCode)"
} catch {
  $sr = $_.Exception.Response
  $code = [int]$sr.StatusCode
  $reader = New-Object System.IO.StreamReader($sr.GetResponseStream())
  $content = $reader.ReadToEnd() | ConvertFrom-Json
  Check 'DEF-001-json' (($code -eq 400) -and ($content.code -eq 'VALIDATION_ERROR')) "status=$code code=$($content.code)"
}

Write-Host '== DEF-005 teacher rag =='
$r = Api 'POST' '/api/ai/lexical-rag/query' $teacher @{ query = 'What is the difference between English coin and French coin?' }
$j = $r.body | ConvertFrom-Json
$r.body | Set-Content -Encoding utf8 "$OutDir/teacher-rag.json"
Check 'DEF-005-teacher-rag' ($r.status -eq 200 -and $j.success -eq $true) "status=$($r.status) source=$($j.data.generationSource) reason=$($j.data.fallbackReason) detail=$($j.data.fallbackDetail)"

Write-Host '== student rag =='
$r = Api 'POST' '/api/ai/lexical-rag/query' $student @{ query = 'What is the difference between English coin and French coin?' }
$j = $r.body | ConvertFrom-Json
$r.body | Set-Content -Encoding utf8 "$OutDir/student-rag.json"
Check 'student-rag' ($r.status -eq 200) "status=$($r.status) source=$($j.data.generationSource) grounded=$($j.data.grounded) reason=$($j.data.fallbackReason) detail=$($j.data.fallbackDetail) latency=$($j.data.latencyMs)"

Write-Host '== recommend (sync) =='
$r = Api 'POST' '/api/ai/recommend-training' $student @{ diagnosisSummaryId = 1 }
$j = $r.body | ConvertFrom-Json
$r.body | Set-Content -Encoding utf8 "$OutDir/recommend.json"
Check 'recommend-sync' ($r.status -eq 200 -and $r.body -notmatch 'error code: 504') "status=$($r.status) source=$($j.data.generationSource) reason=$($j.data.fallbackReason) detail=$($j.data.fallbackDetail) latency=$($j.data.latencyMs)"

Write-Host '== explain async =='
$r = Api 'POST' '/api/ai/explain-diagnosis/async' $student @{ diagnosisSummaryId = 1 }
$j = $r.body | ConvertFrom-Json
$jobId = $j.data.jobId
Check 'explain-async-submit' (($r.status -eq 200 -or $r.status -eq 202) -and $jobId) "status=$($r.status) jobId=$jobId statusField=$($j.data.status)"
if ($jobId) {
  $final = $null
  for ($i = 0; $i -lt 90; $i++) {
    Start-Sleep -Seconds 2
    $jr = Api 'GET' "/api/ai/jobs/$jobId" $student
    $jj = $jr.body | ConvertFrom-Json
    if ($jj.data.status -in @('SUCCEEDED','FAILED')) { $final = $jj; break }
  }
  $final | ConvertTo-Json -Depth 12 | Set-Content -Encoding utf8 "$OutDir/explain-async-job.json"
  $ok = $final -and $final.data.status -eq 'SUCCEEDED'
  Check 'explain-async-job' $ok "status=$($final.data.status) gen=$($final.data.result.generationSource) reason=$($final.data.result.fallbackReason) detail=$($final.data.result.fallbackDetail)"
}

Write-Host '== health =='
$r = Api 'GET' '/api/admin/ai/health' $admin
# try common paths if needed
if ($r.status -ne 200) {
  $r = Api 'GET' '/api/admin/ai-config/health' $admin
}
$r.body | Set-Content -Encoding utf8 "$OutDir/ai-health.json"
$j = $null
try { $j = $r.body | ConvertFrom-Json } catch {}
Check 'ai-health-http' ($r.status -eq 200) "status=$($r.status) bodySnippet=$($r.body.Substring(0, [Math]::Min(180, $r.body.Length)))"

Write-Host '== spa shell =='
$spa = Invoke-WebRequest -Uri $Base -UseBasicParsing
Check 'spa' ($spa.StatusCode -eq 200) "status=$($spa.StatusCode)"

$pass = @($results | Where-Object { $_.ok }).Count
$fail = @($results | Where-Object { -not $_.ok }).Count
Write-Host "SUMMARY pass=$pass fail=$fail"
$results | ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 "$OutDir/summary.json"
if ($fail -gt 0) { exit 1 }
