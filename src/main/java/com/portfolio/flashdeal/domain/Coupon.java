package com.portfolio.flashdeal.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 쿠폰 엔티티
 * - availableStock: 초기 재고 (예: 100개)
 * - issuedCount: 현재까지 발급된 쿠폰 개수
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer availableStock;

    @Column(nullable = false)
    private Integer issuedCount;

    @Builder
    public Coupon(String name, Integer availableStock) {
        this.name = name;
        this.availableStock = availableStock;
        this.issuedCount = 0;
    }

    /**
     * 쿠폰 발급 가능 여부 확인
     */
    public boolean canIssue() {
        return issuedCount < availableStock;
    }

    /**
     * 쿠폰 발급 처리 (재고 차감)
     * @throws IllegalStateException 재고가 부족할 경우
     */
    public void issue() {
        if (!canIssue()) {
            throw new IllegalStateException(
                String.format("재고 부족: 사용 가능 재고=%d, 발급된 수=%d", 
                    availableStock, issuedCount)
            );
        }
        this.issuedCount++;
    }

    /**
     * 남은 재고 수량
     */
    public int getRemainingStock() {
        return availableStock - issuedCount;
    }
}
