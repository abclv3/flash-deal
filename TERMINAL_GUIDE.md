# 🚀 터미널 완벽 실행 가이드 (CMD)

## ✅ 준비 단계 (최초 1회만)

### 1. Docker Desktop 설치 및 실행

**Docker Desktop이 없다면:**
1. https://www.docker.com/products/docker-desktop/ 에서 다운로드
2. 설치 후 재부팅
3. Docker Desktop 실행 (트레이 아이콘 확인)

**Docker Desktop이 이미 설치되어 있다면:**
- 시작 메뉴에서 "Docker Desktop" 검색 후 실행
- 우측 하단 트레이 아이콘이 녹색으로 변경될 때까지 대기 (약 30초~1분)

---

## 🎯 터미널 실행 (CMD - 3단계)

### ⚠️ 중요: PowerShell이 아닌 **CMD**(명령 프롬프트)를 사용하세요!

**CMD 실행 방법:**
1. `Win + R` → `cmd` 입력 → Enter
2. 또는 시작 메뉴에서 "명령 프롬프트" 또는 "cmd" 검색

---

### STEP 1: Redis 실행

```cmd
:: 프로젝트 디렉토리로 이동
cd C:\di_portfolio\flash-deal

:: Redis 컨테이너 실행
docker-compose up -d

:: Redis 실행 확인 (flash-deal-redis 컨테이너가 보여야 함)
docker ps
```

**예상 결과:**
```
CONTAINER ID   IMAGE            STATUS              PORTS                    NAMES
abc123def456   redis:7-alpine   Up 10 seconds       0.0.0.0:6379->6379/tcp   flash-deal-redis
```

**✅ "flash-deal-redis" 컨테이너가 "Up" 상태면 성공!**

---

### STEP 2: 테스트 실행 (핵심!)

```cmd
:: 동시성 테스트 실행
gradlew.bat test --tests CouponConcurrencyTest

:: 또는 모든 테스트 실행
gradlew.bat test
```

**예상 결과:**
```
> Task :test

CouponConcurrencyTest > V1: 동시성 제어 없이 쿠폰 발급 시 Race Condition 발생 PASSED
CouponConcurrencyTest > V2: Redisson 분산 락을 사용한 안전한 쿠폰 발급 PASSED
CouponConcurrencyTest > 성능 비교: V1 vs V2 PASSED

BUILD SUCCESSFUL in 25s
```

---

### STEP 3: 웹 애플리케이션 실행 (선택사항)

```cmd
:: Spring Boot 실행
gradlew.bat bootRun
```

그 다음 브라우저에서 접속:
```
http://localhost:8080
```

**종료 방법:** `Ctrl + C`

---

## 📝 자주 사용하는 CMD 명령어

### 빌드 관련

```cmd
:: 프로젝트 빌드 (테스트 제외)
gradlew.bat clean build -x test

:: 프로젝트 빌드 (테스트 포함)
gradlew.bat clean build
```

### 테스트 관련

```cmd
:: 모든 테스트 실행
gradlew.bat test

:: 특정 테스트만 실행
gradlew.bat test --tests CouponConcurrencyTest

:: 특정 테스트 메서드만 실행
gradlew.bat test --tests CouponConcurrencyTest.testConcurrency_V1_Unsafe

:: 상세 로그와 함께 테스트
gradlew.bat test --info
```

### Redis 관련

```cmd
:: Redis 시작
docker-compose up -d

:: Redis 상태 확인
docker ps

:: Redis 로그 확인
docker-compose logs redis

:: Redis 중지
docker-compose down

:: Redis 완전 삭제 (데이터 포함)
docker-compose down -v

:: Redis 재시작
docker-compose restart redis
```

### Spring Boot 실행

```cmd
:: 애플리케이션 실행
gradlew.bat bootRun

:: 특정 프로파일로 실행
gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

---

## 🔍 트러블슈팅

### ❌ "Docker is not running" 또는 "Cannot connect to Docker daemon"

**문제:** Docker Desktop이 실행되지 않음

**해결 방법:**
```cmd
:: 1. Docker Desktop 실행 (시작 메뉴에서 수동 실행)
:: 2. 트레이 아이콘이 녹색이 될 때까지 대기 (30초~1분)
:: 3. Docker 상태 확인
docker ps

:: 위 명령어가 정상 작동하면 OK!
```

---

### ❌ "Redis connection refused" 또는 테스트 실패

**문제:** Redis가 실행되지 않거나 연결 실패

**해결 방법:**
```cmd
:: Redis 상태 확인
docker ps | findstr redis

:: Redis가 안 보이면 실행
docker-compose up -d

:: 5초 대기 후 확인
timeout /t 5
docker ps

:: Redis 로그 확인 (에러 파악)
docker-compose logs redis

:: Redis 재시작
docker-compose restart redis

:: 그래도 안되면 완전 재설치
docker-compose down -v
docker-compose up -d
```

---

### ❌ 테스트 실패 (V1 또는 V2 FAILED)

**문제:** Redis 연결 타임아웃 또는 동시성 이슈

**해결 방법:**
```cmd
:: 1. Redis가 정상 작동하는지 확인
docker ps | findstr redis

:: 2. Redis 재시작
docker-compose restart redis

:: 3. 5초 대기
timeout /t 5

:: 4. 테스트 재실행
gradlew.bat clean test --tests CouponConcurrencyTest

:: 5. 그래도 실패하면 상세 로그 확인
gradlew.bat test --tests CouponConcurrencyTest --info --stacktrace
```

**일반적인 실패 원인:**
1. **Redis 미실행** → `docker-compose up -d` 실행
2. **Redis 연결 지연** → 5초 대기 후 재시도
3. **포트 충돌 (6379)** → 다른 Redis가 실행 중인지 확인
4. **방화벽 차단** → Windows 방화벽에서 Docker 허용

---

### ❌ Gradle Daemon 문제

**문제:** "Gradle Daemon stopped unexpectedly"

**해결 방법:**
```cmd
:: Gradle Daemon 중지
gradlew.bat --stop

:: 5초 대기
timeout /t 5

:: 다시 시도
gradlew.bat test
```

---

### ❌ "gradlew.bat: command not found"

**문제:** 잘못된 디렉토리에 있음

**해결 방법:**
```cmd
:: 현재 위치 확인
cd

:: 프로젝트 디렉토리로 이동
cd C:\di_portfolio\flash-deal

:: gradlew.bat 파일이 있는지 확인
dir gradlew.bat

:: 있으면 OK, 없으면 경로 확인
```

---

## 🎬 처음부터 끝까지 완전 실행 예제 (복사 & 붙여넣기)

```cmd
@echo off
echo ===== Flash Deal 프로젝트 실행 =====
echo.

:: 1. 프로젝트 디렉토리로 이동
echo [1/6] 프로젝트 디렉토리로 이동...
cd C:\di_portfolio\flash-deal
echo 현재 위치: %CD%
echo.

:: 2. Docker Desktop 실행 확인
echo [2/6] Docker 상태 확인...
docker ps >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Docker가 실행되지 않았습니다!
    echo → 시작 메뉴에서 "Docker Desktop"을 실행하고 30초 대기 후 다시 시도하세요.
    pause
    exit /b 1
)
echo ✅ Docker 정상 작동 중
echo.

:: 3. Redis 실행
echo [3/6] Redis 컨테이너 실행...
docker-compose up -d
echo.

:: 4. Redis 확인
echo [4/6] Redis 상태 확인 (5초 대기)...
timeout /t 5 /nobreak
docker ps | findstr flash-deal-redis
echo.

:: 5. 프로젝트 빌드
echo [5/6] 프로젝트 빌드 중...
gradlew.bat clean build -x test
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 빌드 실패!
    pause
    exit /b 1
)
echo ✅ 빌드 성공
echo.

:: 6. 동시성 테스트 실행
echo [6/6] 동시성 테스트 실행 중...
gradlew.bat test --tests CouponConcurrencyTest
echo.

echo ===== 실행 완료 =====
pause
```

**위 스크립트를 `run.bat` 파일로 저장하면 더블클릭만으로 실행 가능!**

---

## 📊 예상 실행 시간

| 작업 | 소요 시간 |
|------|----------|
| Redis 시작 | 5~10초 |
| 프로젝트 빌드 (최초) | 30~60초 |
| 프로젝트 빌드 (2회차 이후) | 5~15초 |
| 테스트 실행 | 20~30초 |
| Spring Boot 시작 | 10~20초 |

---

## ✨ 면접관에게 보여줄 때 (빠른 데모)

```cmd
:: 1. Redis 실행 (백그라운드)
docker-compose up -d

:: 2. 5초 대기
timeout /t 5

:: 3. 동시성 테스트 1개만 실행 (빠르게)
gradlew.bat test --tests CouponConcurrencyTest.testConcurrency_V2_Safe_WithRedissonLock

:: 4. 웹 UI로 데모
gradlew.bat bootRun
:: → http://localhost:8080 접속
```

---

## 🆚 PowerShell vs CMD 차이점

| 기능 | PowerShell | CMD |
|------|-----------|-----|
| Gradle 실행 | `.\gradlew.bat test` | `gradlew.bat test` (간단!) |
| 파일 찾기 | `Get-ChildItem` | `dir` |
| 필터링 | `Where-Object` | `findstr` (간단!) |
| 명령 체인 | `\|` (복잡) | `\|` (직관적) |

**결론: 이 프로젝트는 CMD가 더 간단하고 직관적입니다!**

---

## 💡 팁

- `gradlew.bat` 대신 `gradlew`만 입력해도 됩니다 (Windows가 자동으로 .bat 추가)
- 명령어가 길면 `Tab` 키로 자동완성 가능
- 실행 중인 명령 종료는 `Ctrl + C`
- `cls` 명령으로 화면 정리
- `도스창이 닫히는 것 방지:` 마지막에 `pause` 추가

---

## 🚨 긴급 복구 (모든 게 안될 때)

```cmd
:: 1. 모든 Docker 컨테이너 정리
docker-compose down -v

:: 2. Gradle 캐시 정리
gradlew.bat clean

:: 3. Gradle Daemon 중지
gradlew.bat --stop

:: 4. 5초 대기
timeout /t 5

:: 5. Docker Desktop 재시작 (수동)
:: → 트레이 아이콘 우클릭 → Restart

:: 6. 10초 대기
timeout /t 10

:: 7. 처음부터 다시
docker-compose up -d
timeout /t 5
gradlew.bat clean build
gradlew.bat test
```

---

**📌 이 가이드를 출력하거나 모니터 옆에 두고 실행하면 편합니다!**
