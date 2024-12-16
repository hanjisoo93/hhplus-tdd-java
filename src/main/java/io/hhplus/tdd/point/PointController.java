package io.hhplus.tdd.point;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    // 특정 유저 포인트 조회
    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable long id
    ) {
        return pointService.selectPoint(id);
    }

    // 특정 유저 포인트 충전/이용 내역 조회
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable long id
    ) {
        return pointService.selectPointHistory(id);
    }

    /**
     * 특정 유저 포인트 충전
     * 성공 케이스
     * 1. 유저가 요청한 금액 만큼 정상적으로 충전이 이루어진 경우 > 잔액 = 기존 잔액 + 충전 금액
     * 2. 최소 충전 금액 조건 충족 > 최소 충전 금액 1,000원
     * 3. 최대 충전 한도 내 충전 > 최대 충전 가능 금액 100,000원
     * 4. 포인트 충전 시 잔액이 1,000,000원 이하일 경우
     * 실패 케이스
     * 1. 유저가 요청한 금액이 0인 경우
     * 2. 최소 충전 금액 미만 > 최소 충전 금액 1,000원
     * 3. 최대 충전 한도 초과 > 최대 충전 가능 금액 100,000원
     * 4. 포인트 충전 시 잔액이 1,000,000원 초과 일 경우
     * 5. 시스템 에러 > DB 오류, 네트워크 에러 등
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        return pointService.insertPoint(id, amount);
    }

    /**
     * 특정 유저 포인트 사용
     * 성공 케이스
     * 1. 사용 가능 잔고 충족 > 잔액 >= 기존 잔액 - 사용 금액
     * 2. 최소 사용 조건 충족 > 최소 사용 가능 금액 1,000원
     * 3. 특정 서비스의 사용 조건 충족 > 할인 프로모션 조건을 만족한 경우 포인트 사용 성공
     * 실패 케이스
     * 1. 잔고 부족 > 잔액 < 사용 요청 금액
     * 2. 0 미만 금액 사용 요청
     * 3. 최소 사용 조건 미만 > 최소 사용 가능 금액 1,000원
     * 4. 지정된 사용 가능 조건 위반 > 특정 상품에만 사용 가능한 포인트
     * 5. 사용 한도 초과 > 1회 최대 사용 가능 금액 초과하는 경우 500,000원
     * 6. 유효하지 않은 유저 상태
     * 7. 시스템 에러 > DB 오류, 네트워크 에러 등
     */
    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        return pointService.updatePoint(id, amount);
    }
}
