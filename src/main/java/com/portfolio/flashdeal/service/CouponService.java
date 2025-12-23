package com.portfolio.flashdeal.service;

import com.portfolio.flashdeal.domain.Coupon;
import com.portfolio.flashdeal.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 쿠폰 발급 서비스
 * V1: 동시성 제어 없이 발급 (Race Condition 발생)
 * V2: Redisson 분산 락을 사용한 안전한 발급
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY_PREFIX = "coupon:lock:";

    /**
     * V1: 동시성 제어 없는 쿠폰 발급 (UNSAFE)
     * 
     * 문제점:
     * - 여러 스레드가 동시에 canIssue() 체크 시 모두 true를 받을 수 있음
     * - 실제로는 100개만 발급되어야 하지만 더 많이 발급될 수 있음
     * - DB 레벨의 격리 수준만으로는 완전히 방지할 수 없음
     * 
     * @param couponId 쿠폰 ID
     * @return 발급 성공 여부
     */
    @Transactional
    public boolean issueCouponV1(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        // ⚠️ Race Condition 발생 구간
        // 여러 스레드가 동시에 이 부분을 실행하면
        // 모두 canIssue()가 true를 반환할 수 있음
        if (!coupon.canIssue()) {
            log.warn("[V1] 재고 부족: couponId={}, remainingStock={}",
                    couponId, coupon.getRemainingStock());
            return false;
        }

        // 쿠폰 발급 처리
        coupon.issue();
        log.debug("[V1] 쿠폰 발급 완료: couponId={}, issuedCount={}",
                couponId, coupon.getIssuedCount());

        return true;
    }

    /**
     * V2: Redisson 분산 락을 사용한 안전한 쿠폰 발급 (SAFE)
     * 
     * 해결 방법:
     * - Redisson의 RLock을 사용하여 분산 환경에서 동시성 제어
     * - tryLock으로 락 획득 시도 (대기 시간 5초, 점유 시간 3초)
     * - 한 번에 하나의 스레드만 쿠폰 발급 로직을 실행
     * 
     * @param couponId 쿠폰 ID
     * @return 발급 성공 여부
     */
    @Transactional
    public boolean issueCouponV2(Long couponId) {
        String lockKey = LOCK_KEY_PREFIX + couponId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도 (최대 5초 대기, 3초 후 자동 해제)
            boolean isLocked = lock.tryLock(5, 3, TimeUnit.SECONDS);

            if (!isLocked) {
                log.warn("[V2] 락 획득 실패: couponId={}", couponId);
                return false;
            }

            try {
                // 🔒 임계 영역 시작 - 한 번에 하나의 스레드만 실행
                Coupon coupon = couponRepository.findById(couponId)
                        .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

                if (!coupon.canIssue()) {
                    log.warn("[V2] 재고 부족: couponId={}, remainingStock={}",
                            couponId, coupon.getRemainingStock());
                    return false;
                }

                coupon.issue();
                log.debug("[V2] 쿠폰 발급 완료: couponId={}, issuedCount={}",
                        couponId, coupon.getIssuedCount());

                return true;
                // 🔒 임계 영역 종료

            } finally {
                // 락 해제
                lock.unlock();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[V2] 락 획득 중 인터럽트 발생: couponId={}", couponId, e);
            return false;
        }
    }

    /**
     * 쿠폰 생성 (테스트용)
     */
    @Transactional
    public Coupon createCoupon(String name, Integer stock) {
        Coupon coupon = Coupon.builder()
                .name(name)
                .availableStock(stock)
                .build();
        return couponRepository.save(coupon);
    }

    /**
     * 쿠폰 조회
     */
    @Transactional(readOnly = true)
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));
    }
}
