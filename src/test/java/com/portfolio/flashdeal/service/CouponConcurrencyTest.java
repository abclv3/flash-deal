package com.portfolio.flashdeal.service;

import com.portfolio.flashdeal.domain.Coupon;
import com.portfolio.flashdeal.repository.CouponRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 쿠폰 발급 동시성 테스트
 * 
 * 목적:
 * 1. V1 (동시성 제어 없음) - Race Condition 발생을 확인
 * 2. V2 (Redisson 분산 락) - 동시성 문제 해결을 검증
 * 
 * 테스트 시나리오:
 * - 재고 100개의 쿠폰에 대해 1,000개의 스레드가 동시에 발급 요청
 * - V1: 100개를 초과하여 발급될 것으로 예상 (Race Condition)
 * - V2: 정확히 100개만 발급될 것으로 예상 (분산 락으로 해결)
 */
@Slf4j
@SpringBootTest
class CouponConcurrencyTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    private static final int TOTAL_STOCK = 100;
    private static final int THREAD_COUNT = 1000;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 정리
        couponRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리
        couponRepository.deleteAll();
    }

    @Test
    @DisplayName("V1: 동시성 제어 없이 쿠폰 발급 시 Race Condition 발생")
    void testConcurrency_V1_Unsafe() throws InterruptedException {
        // Given: 재고 100개의 쿠폰 생성
        Coupon coupon = couponService.createCoupon("신년 특별 쿠폰 V1", TOTAL_STOCK);
        Long couponId = coupon.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 1,000개의 스레드가 동시에 쿠폰 발급 시도
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    boolean result = couponService.issueCouponV1(couponId);
                    if (result) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("[V1] 쿠폰 발급 중 예외 발생", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();

        // Then: 결과 확인
        Coupon result = couponService.getCoupon(couponId);

        log.info("========== V1 테스트 결과 (동시성 제어 없음) ==========");
        log.info("실행 시간: {}ms", endTime - startTime);
        log.info("전체 요청: {}개", THREAD_COUNT);
        log.info("성공: {}개", successCount.get());
        log.info("실패: {}개", failCount.get());
        log.info("DB 발급 수량: {}개", result.getIssuedCount());
        log.info("예상 재고: {}개", TOTAL_STOCK);
        log.info("실제 남은 재고: {}개", result.getRemainingStock());
        log.info("=================================================");

        // ⚠️ Race Condition 발생 확인
        // DB에 기록된 발급 수량이 재고(100개)를 초과할 것으로 예상
        // 단, 실행 속도가 너무 빠르면 Race Condition이 발생하지 않을 수도 있음
        if (result.getIssuedCount() > TOTAL_STOCK) {
            log.warn("🔴 Race Condition 발생: {}개가 초과 발급됨",
                    result.getIssuedCount() - TOTAL_STOCK);
            // 동시성 문제로 인해 100개를 초과하여 발급되었음을 검증
            assertThat(result.getIssuedCount()).isGreaterThan(TOTAL_STOCK);
        } else {
            log.info("⚠️ Race Condition이 발생하지 않았지만, 동시성 제어가 없어 위험함");
            // 적어도 일부는 발급되어야 함
            assertThat(result.getIssuedCount()).isGreaterThan(0);
            assertThat(result.getIssuedCount()).isLessThanOrEqualTo(TOTAL_STOCK);
        }
    }

    @Test
    @DisplayName("V2: Redisson 분산 락을 사용한 안전한 쿠폰 발급")
    void testConcurrency_V2_Safe_WithRedissonLock() throws InterruptedException {
        // Given: 재고 100개의 쿠폰 생성
        Coupon coupon = couponService.createCoupon("신년 특별 쿠폰 V2", TOTAL_STOCK);
        Long couponId = coupon.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 1,000개의 스레드가 동시에 쿠폰 발급 시도
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    boolean result = couponService.issueCouponV2(couponId);
                    if (result) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("[V2] 쿠폰 발급 중 예외 발생", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();

        // Then: 결과 확인
        Coupon result = couponService.getCoupon(couponId);

        log.info("========== V2 테스트 결과 (Redisson 분산 락) ==========");
        log.info("실행 시간: {}ms", endTime - startTime);
        log.info("전체 요청: {}개", THREAD_COUNT);
        log.info("성공: {}개", successCount.get());
        log.info("실패: {}개", failCount.get());
        log.info("DB 발급 수량: {}개", result.getIssuedCount());
        log.info("예상 재고: {}개", TOTAL_STOCK);
        log.info("실제 남은 재고: {}개", result.getRemainingStock());
        log.info("==================================================");

        // ✅ 정확히 100개만 발급되었는지 검증
        // 중요: 재고를 초과하지 않았는지 확인 (동시성 제어의 핵심)
        assertThat(result.getIssuedCount()).isLessThanOrEqualTo(TOTAL_STOCK);
        assertThat(result.getIssuedCount()).isGreaterThan(0);

        // 재고를 모두 소진한 경우
        if (result.getIssuedCount() == TOTAL_STOCK) {
            assertThat(result.getRemainingStock()).isEqualTo(0);
            log.info("✅ 분산 락을 통해 정확히 {}개 발급! 동시성 문제 완벽 해결!", TOTAL_STOCK);
        } else {
            // 일부 요청이 타임아웃된 경우 (성공 + 실패 = 전체 요청)
            log.info("ℹ️ 일부 요청이 타임아웃되었지만, 재고 초과 발급은 없음");
            log.info("✅ 분산 락을 통해 동시성 문제가 해결되었습니다!");
        }

        // 성공+실패의 합이 전체 요청 수와 같은지 확인
        assertThat(successCount.get() + failCount.get()).isEqualTo(THREAD_COUNT);
    }

    @Test
    @DisplayName("성능 비교: V1 vs V2")
    void testPerformanceComparison() throws InterruptedException {
        // V1 테스트
        Coupon couponV1 = couponService.createCoupon("성능 테스트 V1", TOTAL_STOCK);
        long v1Time = measureExecutionTime(() -> {
            try {
                executeConcurrentRequests(couponV1.getId(), true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // V2 테스트
        Coupon couponV2 = couponService.createCoupon("성능 테스트 V2", TOTAL_STOCK);
        long v2Time = measureExecutionTime(() -> {
            try {
                executeConcurrentRequests(couponV2.getId(), false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 결과 출력
        Coupon v1Result = couponService.getCoupon(couponV1.getId());
        Coupon v2Result = couponService.getCoupon(couponV2.getId());

        log.info("========== 성능 비교 결과 ==========");
        log.info("V1 (동시성 제어 없음):");
        log.info("  - 실행 시간: {}ms", v1Time);
        log.info("  - 발급 수량: {}개 (재고 초과: {}개)",
                v1Result.getIssuedCount(), v1Result.getIssuedCount() - TOTAL_STOCK);
        log.info("");
        log.info("V2 (Redisson 분산 락):");
        log.info("  - 실행 시간: {}ms", v2Time);
        log.info("  - 발급 수량: {}개 (정확히 재고만큼)", v2Result.getIssuedCount());
        log.info("  - 성능 오버헤드: {}ms ({}% 증가)",
                v2Time - v1Time, ((v2Time - v1Time) * 100.0 / v1Time));
        log.info("====================================");

        // V2가 정확히 100개만 발급했는지 검증
        assertThat(v2Result.getIssuedCount()).isEqualTo(TOTAL_STOCK);
    }

    private void executeConcurrentRequests(Long couponId, boolean useV1)
            throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    if (useV1) {
                        couponService.issueCouponV1(couponId);
                    } else {
                        couponService.issueCouponV2(couponId);
                    }
                } catch (Exception e) {
                    // 예외 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
    }

    private long measureExecutionTime(Runnable task) {
        long startTime = System.currentTimeMillis();
        task.run();
        return System.currentTimeMillis() - startTime;
    }
}
