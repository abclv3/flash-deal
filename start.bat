@echo off
:: 모든 Java 프로세스 종료
echo Java 프로세스 정리 중...
taskkill /F /IM java.exe 2>nul

:: 3초 대기
timeout /t 3 /nobreak >nul

:: Spring Boot 실행
echo.
echo ========================================
echo   Flash-Deal 서버 시작 중...
echo   URL: http://localhost:8080
echo ========================================
echo.
gradlew.bat bootRun
