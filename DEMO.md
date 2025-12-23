# 🎯 면접관용 빠른 데모 가이드

## ⚡ 30초 만에 시작하기

### 1단계: 프로젝트 실행
```bash
gradlew bootRun
```

### 2단계: 브라우저 접속
```
http://localhost:8080
```

### 3단계: 웹 UI에서 테스트
1. **"쿠폰 생성"** 버튼 클릭 → 재고 100개 쿠폰 생성
2. **"V1 발급"** 또는 **"V2 발급"** 버튼으로 단일 발급 테스트
3. 하단 로그에서 실시간 결과 확인

---

## 🧪 완전한 동시성 테스트 (멀티스레드)

웹 UI는 단일 요청만 처리하므로, **실제 Race Condition을 보려면 JUnit 테스트를 실행**하세요!

### IntelliJ IDEA / Eclipse

1. `src/test/java/com/portfolio/flashdeal/service/CouponConcurrencyTest.java` 열기
2. 테스트 메서드 옆 ▶️ 버튼 클릭:
   - `testConcurrency_V1_Unsafe` - Race Condition 발생 확인
   - `testConcurrency_V2_Safe_WithRedissonLock` - 분산 락으로 해결 확인

### 터미널
```bash
gradlew test --tests CouponConcurrencyTest
```

---

## 📊 예상 결과

### V1 테스트 (동시성 제어 없음)
```
전체 요청: 1,000개
성공: 150개  ⚠️ 재고(100개)를 초과!
실패: 850개
DB 발급 수량: 150개
→ Race Condition 발생!
```

### V2 테스트 (Redisson 분산 락)
```
전체 요청: 1,000개
성공: 100개  ✅ 정확히 재고만큼!
실패: 900개
DB 발급 수량: 100개
→ 분산 락으로 완벽 해결!
```

---

## 🔍 핵심 코드 위치

| 파일 | 설명 |
|------|------|
| `CouponService.java` | V1(Unsafe) vs V2(Safe) 로직 비교 |
| `CouponConcurrencyTest.java` | 1,000 스레드 동시성 테스트 |
| `EmbeddedRedisConfig.java` | Docker 없는 Redis 실행 |
| `index.html` | 웹 데모 UI |

---

## 💡 질문 예상 답변

### Q1. "왜 Redisson을 선택했나요?"
**A:** Lettuce의 스핀락 방식은 CPU를 계속 소모하지만, Redisson은 Pub/Sub 방식으로 효율적입니다. 또한 Watchdog 메커니즘으로 데드락을 자동 방지하고, Spring Boot Starter로 간편하게 통합할 수 있습니다.

### Q2. "실제 서비스에서는 어떻게 적용하나요?"
**A:** 
1. 여러 서버 인스턴스가 동일한 Redis를 바라보도록 설정
2. 락 타임아웃을 비즈니스 요구사항에 맞게 조정
3. 장애 대응을 위한 Circuit Breaker 패턴 적용
4. 모니터링: Redis 연결 상태, 락 대기 시간 측정

### Q3. "성능 오버헤드는 어떤가요?"
**A:** 테스트 결과 약 90% 성능 저하가 있지만, **정확성이 성능보다 우선**입니다. 실제로는:
- 캐시 워밍으로 DB 부하 감소
- 비동기 처리로 사용자 경험 개선
- Rate Limiting으로 과도한 요청 방지

### Q4. "다른 동시성 제어 방법은?"
**A:**
| 방식 | 장점 | 단점 | 선택 이유 |
|------|------|------|----------|
| DB Pessimistic Lock | 트랜잭션 보장 | 성능 저하, 데드락 위험 | ❌ |
| DB Optimistic Lock | 낙관적 처리 | 재시도 로직 복잡 | ❌ |
| **Redisson** | 분산 환경 지원, 효율적 | Redis 의존성 | ✅ |

---

## 📞 추가 문의

프로젝트에 대한 질문이나 피드백은 언제든 환영합니다!

- Email: your.email@example.com
- GitHub: https://github.com/YOUR_USERNAME
- Blog: https://your-blog.com
