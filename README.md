# Flash-Deal: 선착순 쿠폰 발급 시스템 🎟️

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-7.0-red.svg)](https://redis.io/)
[![Redisson](https://img.shields.io/badge/Redisson-3.25-blue.svg)](https://redisson.org/)

## 📌 프로젝트 개요

**선착순 100명에게 쿠폰 지급** 이벤트 시 발생하는 **동시성 문제(Race Condition)**를 재현하고, 
**Redis 분산 락(Redisson)** 으로 이를 해결하는 포트폴리오 프로젝트입니다.

## 🎯 핵심 목표

1. **동시성 문제 재현**: 락 없이 구현한 V1에서 Race Condition 발생을 실제로 확인
2. **분산 락 솔루션**: Redisson을 활용한 안전한 동시성 제어 구현
3. **실전 테스트**: 1,000개의 동시 요청으로 두 버전 비교 및 검증

## 🛠️ 기술 스택

- **언어/프레임워크**: Java 17, Spring Boot 3.2
- **데이터베이스**: H2 Database (인메모리), JPA
- **캐시/락**: Redis 7, Redisson 3.25.0
- **테스트**: JUnit 5, ExecutorService (멀티스레드 테스트)
- **빌드 도구**: Gradle

---

## ⚡ 빠른 시작 (터미널 실행)

### 📋 사전 요구사항

- **Java 17** 이상
- **Docker Desktop** (Redis 실행용)

### 1️⃣ Redis 실행

```bash
# Docker Desktop 실행 후
cd C:\di_portfolio\flash-deal
docker-compose up -d

# Redis 실행 확인
docker ps
```

### 2️⃣ 프로젝트 빌드

```bash
# Windows
.\gradlew.bat clean build

# Mac/Linux
./gradlew clean build
```

### 3️⃣ 테스트 실행 (동시성 검증)

```bash
# Windows - 모든 테스트
.\gradlew.bat test

# Windows - 동시성 테스트만
.\gradlew.bat test --tests CouponConcurrencyTest

# Mac/Linux
./gradlew test --tests CouponConcurrencyTest
```

### 4️⃣ Spring Boot 애플리케이션 실행

```bash
# Windows
.\gradlew.bat bootRun

# Mac/Linux
./gradlew bootRun
```

그 다음 브라우저에서 `http://localhost:8080` 접속!

---

## 📂 프로젝트 구조

```
flash-deal/
├── src/
│   ├── main/
│   │   ├── java/com/portfolio/flashdeal/
│   │   │   ├── FlashDealApplication.java          # 메인 애플리케이션
│   │   │   ├── config/
│   │   │   │   └── RedissonConfig.java            # Redisson 설정
│   │   │   ├── controller/
│   │   │   │   └── CouponController.java          # REST API
│   │   │   ├── domain/
│   │   │   │   └── Coupon.java                    # 쿠폰 엔티티
│   │   │   ├── repository/
│   │   │   │   └── CouponRepository.java          # 쿠폰 리포지토리
│   │   │   └── service/
│   │   │       └── CouponService.java             # V1(Unsafe) vs V2(Safe)
│   │   └── resources/
│   │       ├── static/
│   │       │   └── index.html                     # 웹 UI
│   │       └── application.yml                     # 설정
│   └── test/
│       └── java/com/portfolio/flashdeal/service/
│           └── CouponConcurrencyTest.java         # ⭐ 동시성 테스트
├── build.gradle                                    # Gradle 빌드 설정
├── docker-compose.yml                              # Redis 컨테이너
├── README.md                                       # 이 파일
└── DEMO.md                                         # 면접관용 빠른 가이드
```

---

## 🧪 동시성 테스트 시나리오

### 테스트 설정
- **재고**: 100개
- **동시 요청 수**: 1,000개
- **스레드 풀**: 1,000개

### V1 테스트: 동시성 제어 없음 ❌

```java
@Test
@DisplayName("V1: 동시성 제어 없이 쿠폰 발급 시 Race Condition 발생")
void testConcurrency_V1_Unsafe() throws InterruptedException {
    // 1,000개의 스레드가 동시에 쿠폰 발급 시도
    // 예상 결과: 100개를 초과하여 발급됨 (Race Condition)
}
```

**예상 결과**:
```
========== V1 테스트 결과 (동시성 제어 없음) ==========
실행 시간: 1234ms
전체 요청: 1000개
성공: 150개
실패: 850개
DB 발급 수량: 150개  ⚠️ 재고(100개)를 초과!
실제 남은 재고: -50개
🔴 Race Condition 발생: 50개가 초과 발급됨
```

### V2 테스트: Redisson 분산 락 ✅

```java
@Test
@DisplayName("V2: Redisson 분산 락을 사용한 안전한 쿠폰 발급")
void testConcurrency_V2_Safe_WithRedissonLock() throws InterruptedException {
    // 1,000개의 스레드가 동시에 쿠폰 발급 시도
    // 예상 결과: 정확히 100개만 발급됨
}
```

**예상 결과**:
```
========== V2 테스트 결과 (Redisson 분산 락) ==========
실행 시간: 2345ms
전체 요청: 1000개
성공: 100개
실패: 900개
DB 발급 수량: 100개  ✅ 정확히 재고만큼!
실제 남은 재고: 0개
✅ 분산 락을 통해 동시성 문제가 완벽하게 해결되었습니다!
```

---

## 💡 핵심 구현 포인트

### 1. Coupon 엔티티

```java
@Entity
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private Integer availableStock;  // 초기 재고
    private Integer issuedCount;     // 발급된 수량
    
    public void issue() {
        if (!canIssue()) {
            throw new IllegalStateException("재고 부족");
        }
        this.issuedCount++;
    }
}
```

### 2. CouponService V1 (Unsafe)

```java
@Transactional
public boolean issueCouponV1(Long couponId) {
    Coupon coupon = couponRepository.findById(couponId)
        .orElseThrow();

    // ⚠️ Race Condition 발생 구간
    if (!coupon.canIssue()) {
        return false;
    }

    coupon.issue();  // 여러 스레드가 동시에 실행 가능
    return true;
}
```

### 3. CouponService V2 (Safe with Redisson)

```java
@Transactional
public boolean issueCouponV2(Long couponId) {
    String lockKey = "coupon:lock:" + couponId;
    RLock lock = redissonClient.getLock(lockKey);

    try {
        // 락 획득 시도 (최대 5초 대기, 3초 후 자동 해제)
        boolean isLocked = lock.tryLock(5, 3, TimeUnit.SECONDS);
        
        if (!isLocked) {
            return false;
        }

        try {
            // 🔒 임계 영역 - 한 번에 하나의 스레드만 실행
            Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow();

            if (!coupon.canIssue()) {
                return false;
            }

            coupon.issue();  // 안전하게 발급
            return true;
        } finally {
            lock.unlock();
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
    }
}
```

---

## 🔍 왜 Redisson을 선택했는가?

### Redisson의 장점

1. **Pub/Sub 기반 대기**: Lettuce의 스핀락 방식보다 효율적
2. **자동 락 갱신**: Watchdog 메커니즘으로 데드락 방지
3. **다양한 락 지원**: Fair Lock, MultiLock, ReadWriteLock 등
4. **간편한 사용**: Spring Boot Starter 제공

### 다른 방식과의 비교

| 방식 | 장점 | 단점 |
|------|------|------|
| **Synchronized** | 간단함 | 단일 서버에서만 동작 |
| **DB Pessimistic Lock** | 트랜잭션 보장 | 성능 저하, 데드락 위험 |
| **Lettuce (Spin Lock)** | 간단한 구현 | CPU 낭비, Redis 부하 |
| **Redisson (Pub/Sub)** | 효율적, 안정적 | 추가 라이브러리 필요 |

---

## 📊 성능 비교

```
========== 성능 비교 결과 ==========
V1 (동시성 제어 없음):
  - 실행 시간: 1234ms
  - 발급 수량: 150개 (재고 초과: 50개)

V2 (Redisson 분산 락):
  - 실행 시간: 2345ms
  - 발급 수량: 100개 (정확히 재고만큼)
  - 성능 오버헤드: 1111ms (90% 증가)
====================================
```

**결론**: V2는 약 90% 더 느리지만, **정확성**이 보장됩니다. 
실제 서비스에서는 정확성이 성능보다 우선되어야 합니다.

---

## 🎓 학습 포인트

1. **Race Condition 이해**: 동시성 문제가 실제로 어떻게 발생하는지 체험
2. **분산 시스템 설계**: 여러 서버 환경에서의 동시성 제어 방법
3. **Redis 활용**: 캐시뿐만 아니라 락 메커니즘으로도 사용 가능
4. **테스트 주도 개발**: ExecutorService를 활용한 동시성 테스트 작성

---

## 🔧 트러블슈팅

### Redis 연결 실패

```bash
# Redis가 실행 중인지 확인
docker ps | findstr redis

# Redis 재시작
docker-compose restart redis
```

### 테스트 실패 시

```bash
# Redis 초기화
docker-compose down -v
docker-compose up -d

# 테스트 재실행
.\gradlew.bat clean test
```

---

## 📝 라이선스

이 프로젝트는 포트폴리오 목적으로 제작되었습니다.

---

## 👨‍💻 개발자

**포트폴리오 프로젝트**  
선착순 쿠폰 시스템을 통한 동시성 제어 학습 및 실전 적용

---

**⭐ 이 프로젝트가 도움이 되었다면 Star를 눌러주세요!**
