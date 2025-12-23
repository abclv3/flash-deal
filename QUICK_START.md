# ⚡ Flash-Deal 빠른 실행 가이드

## 🚀 가장 간단한 실행 방법

### 방법 1: start.bat 더블클릭 (제일 쉬움!)

```
파일 탐색기에서:
C:\di_portfolio\flash-deal\start.bat
더블클릭!
```

### 방법 2: CMD에서 실행

```cmd
cd C:\di_portfolio\flash-deal
start.bat
```

---

## ⚠️ 포트 8080 충돌 에러가 계속 날 때

**증상:**
```
Port 8080 was already in use
```

**해결:**
```cmd
:: 1. 모든 Java 프로세스 종료
taskkill /F /IM java.exe

:: 2. 3초 대기
timeout /t 3

:: 3. 다시 실행
start.bat
```

---

## 📖 실행 후 확인

서버가 시작되면 아래 메시지가 보입니다:

```
Started FlashDealApplication in XX.XXX seconds
Tomcat started on port 8080 (http)
```

**브라우저에서 접속:**
```
http://localhost:8080
```

---

## 🛑 서버 종료

터미널에서:
```
Ctrl + C
```

---

## 📝 실행 순서

1. **Redis 확인** (Docker Desktop에서 redis 컨테이너 실행 중인지 확인)
2. **start.bat 실행** (더블클릭 또는 CMD에서 실행)
3. **브라우저 접속** (http://localhost:8080)
4. **쿠폰 생성** → **V1/V2 테스트**

---

## 🔧 트러블슈팅

### 문제 1: Redis 연결 실패
```cmd
docker-compose up -d
```

### 문제 2: 포트 충돌
```cmd
taskkill /F /IM java.exe
```

### 문제 3: Gradle 에러
```cmd
gradlew.bat clean
gradlew.bat --stop
```

---

**💡 팁: start.bat은 자동으로 이전 Java 프로세스를 정리하고 실행합니다!**
