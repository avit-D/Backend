# ============================================================
#  FairPilot E2E 스모크 테스트 (AI·결제 제외 전 기능)
#
#  실행:  powershell -ExecutionPolicy Bypass -File .\e2e-smoke.ps1
#  전제:  4개 서버 실행 중 (8080/8081/8082/8083), MySQL·Redis 실행 중,
#         레포 루트 .env 에 DB_PASSWORD 설정, mysql CLI 사용 가능
#
#  흐름:  서버 응답 → 계정(가입·발급·로그인) → 행사/부스/티켓 생성
#         → 예약 → 결제확정 → 네임태그/체크인 → 부스 스캔(ENTRY/EXIT)
#         → 혼잡도 → 통계 → 리포트 → 정원 초과 차단 → 취소 복원
# ============================================================

$ErrorActionPreference = 'Stop'
$run = Get-Date -Format 'MMddHHmmss'

$V  = 'http://localhost:8080'   # api-visitor
$EX = 'http://localhost:8081'   # api-exhibitor
$EA = 'http://localhost:8082'   # api-expo-admin
$PA = 'http://localhost:8083'   # api-platform-admin

# ── .env 로드 (OS 환경변수 우선) ──────────────────────────────
$envMap = @{}
$envPath = Join-Path $PSScriptRoot '.env'
if (Test-Path $envPath) {
    Get-Content $envPath -Encoding UTF8 | ForEach-Object {
        $l = $_.Trim()
        if ($l -and -not $l.StartsWith('#')) {
            $i = $l.IndexOf('=')
            if ($i -gt 0) { $envMap[$l.Substring(0, $i).Trim()] = $l.Substring($i + 1).Trim() }
        }
    }
}
function EnvVal($k, $d) {
    $os = [Environment]::GetEnvironmentVariable($k)
    if ($os) { return $os }
    if ($envMap.ContainsKey($k)) { return $envMap[$k] }
    return $d
}
$dbAddr = EnvVal 'DB_ADDRESS' 'localhost:3306'
$dbHost = ($dbAddr -split ':')[0]
$dbPort = if ($dbAddr -match ':') { ($dbAddr -split ':')[1] } else { '3306' }
$dbName = EnvVal 'DB_NAME' 'fairpilot'
$dbUser = EnvVal 'DB_USERNAME' 'root'
$dbPw   = EnvVal 'DB_PASSWORD' $null
if (-not $dbPw) { Write-Host '[중단] DB_PASSWORD 미설정 - .env 확인' -ForegroundColor Red; exit 1 }

function Sql($q) {
    & mysql "--host=$dbHost" "--port=$dbPort" "-u$dbUser" "-p$dbPw" -D $dbName --skip-column-names -e $q 2>$null
}

# ── 공통 헬퍼 ────────────────────────────────────────────────
function Api($method, $url, $body = $null, $token = $null, $uid = $null) {
    $h = @{}
    if ($token) { $h['Authorization'] = "Bearer $token" }
    if ($uid)   { $h['X-User-Id'] = "$uid" }
    $p = @{ Method = $method; Uri = $url; Headers = $h }
    if ($null -ne $body) {
        $p['Body'] = [System.Text.Encoding]::UTF8.GetBytes((ConvertTo-Json $body -Depth 6))
        $p['ContentType'] = 'application/json; charset=utf-8'
    }
    return Invoke-RestMethod @p
}
function Login($email, $pw) {
    $r = Api 'Post' "$V/api/auth/login" @{ email = $email; password = $pw }
    return $r.data.accessToken
}

$script:n = 0; $script:pass = 0; $script:fail = 0; $script:failList = @()
function Step($name, $fn) {
    $script:n++
    try {
        & $fn
        Write-Host ('[PASS] {0:d2} {1}' -f $script:n, $name) -ForegroundColor Green
        $script:pass++
    } catch {
        $msg = $_.Exception.Message
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) { $msg = $_.ErrorDetails.Message }
        Write-Host ('[FAIL] {0:d2} {1}' -f $script:n, $name) -ForegroundColor Red
        Write-Host ('       -> ' + $msg) -ForegroundColor DarkYellow
        $script:fail++
        $script:failList += ('{0:d2} {1}' -f $script:n, $name)
    }
}
function StepExpectFail($name, $fn) {
    $script:n++
    $rejected = $false
    try { & $fn | Out-Null } catch { $rejected = $true }
    if ($rejected) {
        Write-Host ('[PASS] {0:d2} {1} (의도된 거부 확인)' -f $script:n, $name) -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host ('[FAIL] {0:d2} {1} - 거부되어야 하는데 성공함' -f $script:n, $name) -ForegroundColor Red
        $script:fail++
        $script:failList += ('{0:d2} {1}' -f $script:n, $name)
    }
}

$pw1 = 'password123!'
$adminEmail   = "e2e$run.admin@test.com"
$expoEmail    = "e2e$run.expo@test.com"
$exhEmail     = "e2e$run.exhibitor@test.com"
$visitor1Email = "e2e$run.visitor1@test.com"
$visitor2Email = "e2e$run.visitor2@test.com"

Write-Host ''
Write-Host "=== FairPilot E2E 스모크 테스트 (run=$run) ===" -ForegroundColor Cyan
Write-Host ''

# ── 0. 서버 응답 ─────────────────────────────────────────────
Step '서버 응답 확인 - visitor(8080)'        { Api 'Get' "$V/api/exhibitions"  | Out-Null }
Step '서버 응답 확인 - exhibitor(8081)'      { Api 'Get' "$EX/api/exhibitions" | Out-Null }
Step '서버 응답 확인 - expo-admin(8082)'     { Api 'Get' "$EA/api/exhibitions" | Out-Null }
Step '서버 응답 확인 - platform-admin(8083)' { Api 'Get' "$PA/api/exhibitions" | Out-Null }

# ── 1. 계정 준비 ─────────────────────────────────────────────
Step '회원가입 - 관리자용/방문자1/방문자2' {
    foreach ($e in @($adminEmail, $visitor1Email, $visitor2Email)) {
        Api 'Post' "$V/api/auth/signup" @{ email = $e; password = $pw1; name = "e2e-$e"; phone = '010-0000-0000' } | Out-Null
    }
}
Step 'PLATFORM_ADMIN 승격 (SQL - 최초 부트스트랩)' {
    Sql "UPDATE users SET role='PLATFORM_ADMIN' WHERE email='$adminEmail';" | Out-Null
    $script:adminId = [long](Sql "SELECT id FROM users WHERE email='$adminEmail';")
    if (-not $script:adminId) { throw 'adminId 조회 실패' }
}
Step '로그인 - PLATFORM_ADMIN' { $script:adminToken = Login $adminEmail $pw1; if (-not $script:adminToken) { throw '토큰 없음' } }

StepExpectFail 'VISITOR 토큰으로 관리자 API 접근 차단' {
    $t = Login $visitor1Email $pw1
    Api 'Get' "$PA/api/admin/accounts" $null $t
}

Step '운영 계정 발급 - EXPO_ADMIN (관리자 API)' {
    $r = Api 'Post' "$PA/api/admin/accounts" @{ email = $expoEmail; name = 'e2e-expo'; phone = '010-1111-1111'; tempPassword = $pw1; role = 'EXPO_ADMIN' } $script:adminToken
    $script:expoId = $r.data.id; if (-not $script:expoId) { throw 'expoId 없음' }
}
Step '운영 계정 발급 - EXHIBITOR (관리자 API)' {
    $r = Api 'Post' "$PA/api/admin/accounts" @{ email = $exhEmail; name = 'e2e-exhibitor'; phone = '010-2222-2222'; tempPassword = $pw1; role = 'EXHIBITOR' } $script:adminToken
    $script:exhId = $r.data.id; if (-not $script:exhId) { throw 'exhId 없음' }
}
Step '로그인 - EXPO_ADMIN/EXHIBITOR/방문자1/방문자2' {
    $script:expoToken = Login $expoEmail $pw1
    $script:exhToken  = Login $exhEmail $pw1
    $script:v1Token   = Login $visitor1Email $pw1
    $script:v2Token   = Login $visitor2Email $pw1
    $script:v1Id = [long](Sql "SELECT id FROM users WHERE email='$visitor1Email';")
    $script:v2Id = [long](Sql "SELECT id FROM users WHERE email='$visitor2Email';")
}

# ── 2. 행사/부스/티켓 (2번 영역) ──────────────────────────────
Step '행사 생성 (PLATFORM_ADMIN)' {
    $today = Get-Date -Format 'yyyy-MM-dd'
    $end   = (Get-Date).AddDays(7).ToString('yyyy-MM-dd')
    $r = Api 'Post' "$PA/api/exhibitions" @{ title = "E2E 테스트 박람회 $run"; slug = "e2e-$run"; venue = '코엑스'; address = '서울'; startDate = $today; endDate = $end } $script:adminToken
    $script:exhibitionId = $r.data.id; if (-not $script:exhibitionId) { throw 'exhibitionId 없음' }
}
Step '부스 생성 (EXPO_ADMIN)' {
    $r = Api 'Post' "$EA/api/exhibitions/$($script:exhibitionId)/booths" @{ exhibitorId = $script:exhId; name = 'E2E 부스'; description = '스모크 테스트용'; tags = 'AI,테스트'; posX = 1; posY = 1; floor = 1 } $script:expoToken
    $script:boothId = $r.data.id; if (-not $script:boothId) { throw 'boothId 없음' }
}
Step '티켓타입 생성 (EXPO_ADMIN)' {
    $r = Api 'Post' "$EA/api/exhibitions/$($script:exhibitionId)/ticket-types" @{ name = '일반'; price = 10000; quota = 100 } $script:expoToken
    $script:ticketTypeId = $r.data.id; if (-not $script:ticketTypeId) { throw 'ticketTypeId 없음' }
}
Step '타임슬롯 시드 (SQL - 생성 API 부재 갭)' {
    Sql "INSERT INTO time_slot (exhibition_id,start_at,end_at,capacity,reserved_count) VALUES ($($script:exhibitionId),NOW(),DATE_ADD(NOW(), INTERVAL 3 HOUR),10,0);" | Out-Null
    $script:slot1 = [long](Sql "SELECT MAX(id) FROM time_slot WHERE exhibition_id=$($script:exhibitionId);")
    Sql "INSERT INTO time_slot (exhibition_id,start_at,end_at,capacity,reserved_count) VALUES ($($script:exhibitionId),NOW(),DATE_ADD(NOW(), INTERVAL 3 HOUR),1,0);" | Out-Null
    $script:slot2 = [long](Sql "SELECT MAX(id) FROM time_slot WHERE exhibition_id=$($script:exhibitionId);")
    if (-not $script:slot1 -or -not $script:slot2) { throw 'slot id 조회 실패' }
}

# ── 3. 예약 (4번 영역) ───────────────────────────────────────
Step '예약 생성 - 방문자1 (INDIVIDUAL)' {
    $body = @{
        exhibitionId = $script:exhibitionId; timeSlotId = $script:slot1; ticketTypeId = $script:ticketTypeId
        movementMode = 'INDIVIDUAL'; groupSize = 1; useQueue = $false
        attendees = @(@{ name = 'e2e참석자1'; phone = '010-3333-3333'; email = $visitor1Email; isGroupLeader = $true })
    }
    $r = Api 'Post' "$V/api/reservations" $body $script:v1Token $script:v1Id
    $script:resId = $r.data.reservationId
    $script:qr1 = $r.data.attendees[0].ticketQrToken
    if (-not $script:qr1) { throw 'ticketQrToken 없음' }
}
Step '결제 확정 처리 (confirm-paid)' {
    Api 'Post' "$V/api/reservations/$($script:resId)/confirm-paid" $null $script:v1Token $script:v1Id | Out-Null
}
Step '내 예약 목록 조회 - 방문자1' {
    $r = Api 'Get' "$V/api/reservations/me" $null $script:v1Token $script:v1Id
    if ($r.data.content.Count -lt 1) { throw '예약 목록 비어 있음' }
}

# ── 4. 네임태그/체크인 (4번 영역) ─────────────────────────────
Step '네임태그 배치 생성 (EXPO_ADMIN)' {
    $r = Api 'Post' "$EA/api/exhibitions/$($script:exhibitionId)/nametags/batch" @{ count = 5 } $script:expoToken
    $script:tag1 = $r.data[0].token; if (-not $script:tag1) { throw '네임태그 토큰 없음' }
}
Step '티켓 QR 검증 (checkin/verify)' {
    Api 'Post' "$EA/api/checkin/verify?exhibitionId=$($script:exhibitionId)&ticketQrToken=$($script:qr1)" $null $script:expoToken | Out-Null
}
Step '네임태그 바인딩 - 체크인 (GATE ENTRY 생성)' {
    $r = Api 'Post' "$EA/api/checkin/nametag" @{ exhibitionId = $script:exhibitionId; ticketQrToken = $script:qr1; nametagToken = $script:tag1 } $script:expoToken $script:expoId
    if (-not $r.data.gateEntryCreated) { throw 'gateEntryCreated=false' }
}
Step '네임태그 재고 요약 조회' {
    Api 'Get' "$EA/api/exhibitions/$($script:exhibitionId)/nametags/summary" $null $script:expoToken | Out-Null
}

# ── 5. 부스 스캔 (4번 영역, api-exhibitor) ────────────────────
Step '스캔 가능 지점 조회 - EXHIBITOR (scan-points)' {
    $r = Api 'Get' "$EX/api/exhibitor/scan-points" $null $script:exhToken
    if ($r.data.Count -lt 1) { throw '스캔 지점 0건 (부스 exhibitorId 매핑 확인 필요)' }
}
Step '부스 스캔 ENTRY' {
    Api 'Post' "$EX/api/visits/scan" @{ nametagToken = $script:tag1; scanPointType = 'BOOTH'; scanPointId = $script:boothId; scanType = 'ENTRY' } $script:exhToken $script:exhId | Out-Null
}
Step '부스 스캔 EXIT (체류 종결)' {
    Start-Sleep -Seconds 2
    Api 'Post' "$EX/api/visits/scan" @{ nametagToken = $script:tag1; scanPointType = 'BOOTH'; scanPointId = $script:boothId; scanType = 'EXIT' } $script:exhToken $script:exhId | Out-Null
}

# ── 6. 혼잡도/통계/리포트 (4번 영역) ──────────────────────────
Step '혼잡도 스냅샷 조회 (congestion/live)' {
    Api 'Get' "$V/api/congestion/live?exhibitionId=$($script:exhibitionId)" $null $script:v1Token | Out-Null
}
Step '통계 배치 리빌드 (stats/rebuild)' {
    Api 'Post' "$EA/api/exhibitions/$($script:exhibitionId)/stats/rebuild" $null $script:expoToken | Out-Null
}
Step '부스 통계 조회 - 방문 집계 반영 확인' {
    $r = Api 'Get' "$EA/api/exhibitions/$($script:exhibitionId)/stats/booths" $null $script:expoToken
    if ($r.data.Count -lt 1) { throw '통계 0건 (visit_log 집계 안 됨)' }
}
Step '방문객 사후 리포트 (my/report)' {
    Api 'Get' "$V/api/my/report?exhibitionId=$($script:exhibitionId)" $null $script:v1Token | Out-Null
}

# ── 7. 동시성/정원 및 취소 (4번 영역 핵심) ────────────────────
Step '정원 1 슬롯 - 방문자1 예약 (정원 소진)' {
    $body = @{
        exhibitionId = $script:exhibitionId; timeSlotId = $script:slot2; ticketTypeId = $script:ticketTypeId
        movementMode = 'INDIVIDUAL'; groupSize = 1; useQueue = $false
        attendees = @(@{ name = 'e2e참석자1'; phone = '010-3333-3333'; email = $visitor1Email; isGroupLeader = $true })
    }
    $r = Api 'Post' "$V/api/reservations" $body $script:v1Token $script:v1Id
    $script:resId2 = $r.data.reservationId
}
StepExpectFail '정원 초과 예약 차단 - 방문자2 (원자 UPDATE 검증)' {
    $body = @{
        exhibitionId = $script:exhibitionId; timeSlotId = $script:slot2; ticketTypeId = $script:ticketTypeId
        movementMode = 'INDIVIDUAL'; groupSize = 1; useQueue = $false
        attendees = @(@{ name = 'e2e참석자2'; phone = '010-4444-4444'; email = $visitor2Email; isGroupLeader = $true })
    }
    Api 'Post' "$V/api/reservations" $body $script:v2Token $script:v2Id
}
Step '예약 취소 - 방문자1 (reserved_count 복원)' {
    Api 'Post' "$V/api/reservations/$($script:resId2)/cancel" $null $script:v1Token $script:v1Id | Out-Null
    $cnt = [int](Sql "SELECT reserved_count FROM time_slot WHERE id=$($script:slot2);")
    if ($cnt -ne 0) { throw "취소 후 reserved_count=$cnt (0이어야 함)" }
}

# ── 결과 요약 ────────────────────────────────────────────────
Write-Host ''
Write-Host '=== 결과 요약 ===' -ForegroundColor Cyan
Write-Host ("통과 {0} / 실패 {1} / 전체 {2}" -f $script:pass, $script:fail, $script:n)
if ($script:failList.Count -gt 0) {
    Write-Host '실패 목록:' -ForegroundColor Red
    $script:failList | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
}
Write-Host ''
Write-Host '[갭] time_slot 생성 API 부재 - SQL 시드로 대체함 (담당 협의 필요)'
Write-Host '[제외] 결제·정산 - domain-payment 가 어느 서버에도 미탑재'
Write-Host '[제외] AI 챗봇·동선 추천 - Ollama 설치 후 별도 테스트'
if ($script:fail -gt 0) { exit 1 } else { exit 0 }
