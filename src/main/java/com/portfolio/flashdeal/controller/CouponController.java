package com.portfolio.flashdeal.controller;

import com.portfolio.flashdeal.domain.Coupon;
import com.portfolio.flashdeal.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 쿠폰 발급 REST API Controller
 * 
 * 목적: 면접관이 웹 브라우저에서 쉽게 테스트할 수 있도록 REST API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * 쿠폰 생성 (테스트용)
     */
    @PostMapping
    public ResponseEntity<Coupon> createCoupon(
            @RequestParam String name,
            @RequestParam Integer stock) {

        Coupon coupon = couponService.createCoupon(name, stock);
        log.info("쿠폰 생성: {}, 재고: {}개", name, stock);
        return ResponseEntity.ok(coupon);
    }

    /**
     * 쿠폰 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<Coupon> getCoupon(@PathVariable Long id) {
        Coupon coupon = couponService.getCoupon(id);
        return ResponseEntity.ok(coupon);
    }

    /**
     * 쿠폰 발급 V1 (동시성 제어 없음 - Race Condition 발생)
     */
    @PostMapping("/{id}/issue-v1")
    public ResponseEntity<Map<String, Object>> issueCouponV1(@PathVariable Long id) {
        boolean result = couponService.issueCouponV1(id);
        Coupon coupon = couponService.getCoupon(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("version", "V1 (동시성 제어 없음)");
        response.put("issuedCount", coupon.getIssuedCount());
        response.put("remainingStock", coupon.getRemainingStock());

        return ResponseEntity.ok(response);
    }

    /**
     * 쿠폰 발급 V2 (Redisson 분산 락 사용 - 안전함)
     */
    @PostMapping("/{id}/issue-v2")
    public ResponseEntity<Map<String, Object>> issueCouponV2(@PathVariable Long id) {
        boolean result = couponService.issueCouponV2(id);
        Coupon coupon = couponService.getCoupon(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("version", "V2 (Redisson 분산 락)");
        response.put("issuedCount", coupon.getIssuedCount());
        response.put("remainingStock", coupon.getRemainingStock());

        return ResponseEntity.ok(response);
    }

    /**
     * 동시성 테스트 실행 (실제 테스트 시뮬레이션)
     */
    @PostMapping("/{id}/stress-test")
    public ResponseEntity<Map<String, Object>> stressTest(
            @PathVariable Long id,
            @RequestParam(defaultValue = "v2") String version,
            @RequestParam(defaultValue = "100") int threadCount) {

        log.info("스트레스 테스트 시작: 쿠폰 ID={}, 버전={}, 스레드={}", id, version, threadCount);

        // 테스트 실행은 CouponService에 위임
        Map<String, Object> result = new HashMap<>();
        result.put("message", "스트레스 테스트는 JUnit 테스트로 실행하세요");
        result.put("testClass", "CouponConcurrencyTest.java");

        return ResponseEntity.ok(result);
    }
}
