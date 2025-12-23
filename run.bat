@echo off
chcp 65001 >nul 2>&1
color 0A
title Flash Deal - 선착순 쿠폰 시스템

echo.
echo ========================================
echo   Flash Deal 선착순 쿠폰 발급 시스템
echo   Redis 분산 락으로 동시성 제어 구현
echo ========================================
echo.

cd /d "%~dp0"
echo [1/5] 현재 위치: %CD%
echo.

echo [2/5] Redis 실행 중...
docker-compose up -d
echo.

echo [3/5] Redis 준비 대기 (5초)...
timeout /t 5 /nobreak >nul
echo      완료!
echo.

echo [4/5] 프로젝트 빌드 중...
echo      (처음 실행 시 약 1분 소요)
call gradlew.bat clean build -x test
if %ERRORLEVEL% NEQ 0 (
    color 0C
    echo      빌드 실패!
    pause
    exit /b 1
)
echo      빌드 성공!
echo.

echo [5/5] 동시성 테스트 실행 중...
echo      (1,000개 스레드 동시 실행, 약 30초 소요)
echo.
call gradlew.bat test --tests CouponConcurrencyTest
echo.

if %ERRORLEVEL% EQU 0 (
    color 0A
    echo ========================================
    echo         모든 테스트 성공!
    echo ========================================
) else (
    color 0E
    echo ========================================
    echo         테스트 실패!
    echo    리포트: build\reports\tests\test\index.html
    echo ========================================
)

echo.
echo.
echo ━━━━━ 추가 실행 옵션 ━━━━━
echo.
echo [1] Spring Boot 웹 서버 실행 (http://localhost:8080)
echo [2] 테스트 리포트 열기 (HTML)
echo [3] Redis 중지
echo [4] 종료
echo.
set /p choice=선택 (1-4): 

if "%choice%"=="1" goto :run_web
if "%choice%"=="2" goto :view_report
if "%choice%"=="3" goto :stop_redis
if "%choice%"=="4" goto :end

:run_web
echo.
echo Spring Boot 실행 중...
echo 브라우저에서 http://localhost:8080 접속
echo Ctrl+C로 종료할 수 있습니다.
echo.
call gradlew.bat bootRun
goto :end

:view_report
echo.
echo 테스트 리포트를 엽니다...
start build\reports\tests\test\index.html
timeout /t 2 >nul
goto :end

:stop_redis
echo.
echo Redis 중지 중...
docker-compose down
echo Redis 중지 완료!
timeout /t 2 >nul
goto :end

:end
echo.
pause
