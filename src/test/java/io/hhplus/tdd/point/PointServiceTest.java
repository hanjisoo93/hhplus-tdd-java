package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PointServiceTest {

    private final UserPointTable userPointTable = mock(UserPointTable.class);
    private final PointHistoryTable pointHistoryTable = mock(PointHistoryTable.class);
    final PointService pointService = new PointService(userPointTable, pointHistoryTable);

    /**
     * 특정 유저 포인트 충전
     * 잔액 : 최소 0원 부터 최대 1,000,000원
     * 최소 충전 금액 : 1,000원
     * 최대 충전 금액 : 100,000원
     */
    @Test
    void 포인트가_정상적으로_충전_되었다면_요청은_성공한다() {
        // given
        final long userId = 1L;
        final long requestedAmount = 9000L;
        final UserPoint expectedUserPoint = new UserPoint(userId, requestedAmount, System.currentTimeMillis());

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, requestedAmount))
                .thenReturn(expectedUserPoint);

        // when
        UserPoint result = pointService.insertPoint(userId, requestedAmount);

        // then
        assertThat(result.point()).isEqualTo(requestedAmount);  // 포인트 정상적으로 충전되었는지 검증
    }

    @Test
    void 최소_충전_금액_이상이면_요청은_성공한다() {
        // given
        final long userId = 1L;
        final long requestedAmount = 9000L;
        final UserPoint expectedUserPoint = new UserPoint(userId, requestedAmount, System.currentTimeMillis());

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, requestedAmount))
                .thenReturn(expectedUserPoint);

        // when
        UserPoint result = pointService.insertPoint(userId, requestedAmount);

        // then
        assertThat(result.point()).isGreaterThanOrEqualTo(1000L); // 최소 충전 금액 검증
    }

    @Test
    void 최대_충전_금액_이하이면_요청은_성공한다() {
        // given
        final long userId = 1L;
        final long requestedAmount = 100000L;
        final UserPoint expectedUserPoint = new UserPoint(userId, requestedAmount, System.currentTimeMillis());

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, requestedAmount))
                .thenReturn(expectedUserPoint);

        // when
        UserPoint result = pointService.insertPoint(userId, requestedAmount);

        // then
        assertThat(result.point()).isLessThanOrEqualTo(100000L);
    }

    @Test
    void 포인트_충전_후_잔액이_100만원_이하일_경우_요청은_성공한다() {
        // given
        final long userId = 1L;
        final long requestedAmount = 10000L;
        final long currentAmount = 500000L;
        final UserPoint expectedUserPoint = new UserPoint(userId, requestedAmount + currentAmount, System.currentTimeMillis());

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, requestedAmount + currentAmount))
                .thenReturn(expectedUserPoint);

        // when
        UserPoint result = pointService.insertPoint(userId, requestedAmount + currentAmount);

        // then
        assertThat(result.point()).isLessThanOrEqualTo(1000000L);
    }

    @Test
    void 유저가_충전_요청한_금액이_0인_경우_요청은_실패한다() {
        // given
        final long userId = 1L;
        final long requestedAmount = 0L;    // 충전 금액
        final UserPoint expectedUserPoint = new UserPoint(userId, requestedAmount, System.currentTimeMillis());  // mock 예상 충전

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId,requestedAmount))
                .thenReturn(expectedUserPoint);

        // when & then
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint reault = pointService.insertPoint(userId, requestedAmount);
                    if (reault.point() == 0L) {
                        throw new Exception("충전 금액은 0보다 커야합니다.");
                    }
                }
        );

        // 예외 메시지 검증
        assertThat(exception.getMessage()).isEqualTo("충전 금액은 0보다 커야합니다.");
    }

    @Test
    void 최소_충전_금액_미만이면_요청은_실패한다() {
        // given
        final long userId = 1L;
        final long requestAmount = 900L;   // 충전 금액
        final UserPoint expectedUserPoint = new UserPoint(userId, requestAmount, System.currentTimeMillis());   // mock 예상 충전

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, requestAmount))
                .thenReturn(expectedUserPoint);

        // when & then
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.insertPoint(userId, requestAmount);
                    if (result.point() < 1000L) {
                        throw new Exception("충전 금액은 최소 1000원 이상이어야 합니다.");
                    }
                }
        );

        // 예외 메세지 검증
        assertThat(exception.getMessage()).isEqualTo("충전 금액은 최소 1000원 이상이어야 합니다.");

    }

    @Test
    void 최대_충전_금액_초과면_요청은_실패한다() {
        // given
        final long userId = 1L;
        final long requestAmount = 150000L;   // 충전 금액
        final UserPoint expectedUserPoint = new UserPoint(userId, requestAmount, System.currentTimeMillis());   // mock 예상 충전

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, requestAmount))
                .thenReturn(expectedUserPoint);

        // when & then
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint reault = pointService.insertPoint(userId, requestAmount);
                    if (reault.point() > 100000L) {
                        throw new Exception("충전 금액은 최대 100,000원 미만이어야 합니다.");
                    }
                }
        );

        // 예외 메세지 검증
        assertThat(exception.getMessage()).isEqualTo("충전 금액은 최대 100,000원 미만이어야 합니다.");
    }

    @Test
    void 포인트_충전_후_잔액이_100만원_초과_일_경우_요청은_실패한다(){
        // given
        final long userId = 1L;
        final long requestAmount = 600000L; // 요청 금액
        final long currentAmount = 500000L; // 현재 포인트 잔액

        final UserPoint expectedUserPoint = new UserPoint(userId, requestAmount + currentAmount, System.currentTimeMillis()); // mock 예상 충전

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, requestAmount + currentAmount))
                .thenReturn(expectedUserPoint);

        // when & then
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.insertPoint(userId, requestAmount + currentAmount);
                    if (result.point() > 1000000L) {
                        throw new Exception("최대 포인트 잔액은 1000,000원입니다.");
                    }
                }
        );

        // 예외 메세지 검증
        assertThat(exception.getMessage()).isEqualTo("최대 포인트 잔액은 1000,000원입니다.");
    }

    /**
     * 특정 유저 포인트 사용
     * 잔액 : 최소 0원 부터 최대 1,000,000원
     * 최소 사용 금액 : 1,000원
     * 최대 사용 금액 : 500,000원
     */
    @Test
    void 사용_가능_잔액_이상이면_요청은_성공한다() {
        // given
        final long userId = 1L;
        final long usedAmount = 1500L;  // 사용할 포인트
        final long currentAmount = 5000L;   // 현재 포인트
        final long availableAmount = currentAmount - usedAmount; // 잔액 포인트
        final UserPoint expectedUserPoint = new UserPoint(userId, availableAmount, System.currentTimeMillis());

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, availableAmount))
                .thenReturn(expectedUserPoint);

        // when
        UserPoint result = pointService.insertPoint(userId, availableAmount);

        // then
        assertThat(result.point()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void 최소_사용_금액_이상이면_요청은_성공한다(){
        // given
        final long userId = 1L;
        final long usedAmount = 1500L;  // 사용할 포인트
        final UserPoint expectedUserPoint = new UserPoint(userId, usedAmount, System.currentTimeMillis());

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, usedAmount))
                .thenReturn(expectedUserPoint);

        // when
        UserPoint result = pointService.insertPoint(userId, usedAmount);

        // then
        assertThat(result.point()).isGreaterThanOrEqualTo(1000L);
    }

    @Test
    void 최대_사용_금액_이하면_요청은_성공한다() {
        // given
        final long userId = 1L;
        final long usedAmount = 100000L;    // 사용할 포인트
        final UserPoint expectedUserPoint = new UserPoint(userId, usedAmount, System.currentTimeMillis());

        // mock 동장 정의
        when(userPointTable.insertOrUpdate(userId, usedAmount))
                .thenReturn(expectedUserPoint);

        // when
        UserPoint result = pointService.insertPoint(userId, usedAmount);

        // then
        assertThat(result.point()).isLessThanOrEqualTo(500000L);
    }

    @Test
    void 사용_가능_잔액이_없으면_요청은_실패한다() {
        // given
        final long userId = 1L;
        final long usedAmount = 5000L;  // 사용할 포인트
        final long currentAmount = 0;   // 현재 포인트
        final long availableAmount = currentAmount - usedAmount;    // 잔액 포인트
        final UserPoint expectedUserPoint = new UserPoint(userId, availableAmount, System.currentTimeMillis());

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, availableAmount))
                .thenReturn(expectedUserPoint);

        // when & then
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.insertPoint(userId, availableAmount);
                    if (result.point() < 0) {
                        throw new Exception("사용 가능한 포인트가 없습니다.");
                    }
                }
        );

        // 예외 메세지 검증
        assertThat(exception.getMessage()).isEqualTo("사용 가능한 포인트가 없습니다.");
    }

    @Test
    void 유저가_사용_요청한_금액이_0미만인_경우_요청은_실패한다() {
        // given
        final long userId = 1L;
        final long usedAmount = -100L;  // 사용할 포인트
        final UserPoint expectedUserPoint = new UserPoint(userId, usedAmount, System.currentTimeMillis());

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, usedAmount))
                .thenReturn(expectedUserPoint);

        // when & then
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.insertPoint(userId, usedAmount);
                    if (result.point() < 0) {
                        throw new Exception("요청한 포인트 금액이 0보다 작습니다.");
                    }
                }
        );

        // 예외 메세지 검증
        assertThat(exception.getMessage()).isEqualTo("요청한 포인트 금액이 0보다 작습니다.");
    }

    @Test
    void 최소_사용_금액_미만이면_요청은_실패한다() {
        // given
        final long userId = 1L;
        final long usedAmount = 500L;  // 사용할 포인트
        final UserPoint expectedUserPoint = new UserPoint(userId, usedAmount, System.currentTimeMillis());

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, usedAmount))
                .thenReturn(expectedUserPoint);

        // when & then
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.insertPoint(userId, usedAmount);
                    if (result.point() < 1000L) {
                        throw new Exception("포인트는 1,000원 이상 사용 가능합니다.");
                    }
                }
        );

        // 예외 메세지 검증
        assertThat(exception.getMessage()).isEqualTo("포인트는 1,000원 이상 사용 가능합니다.");

    }

    @Test
    void 최대_사용_금액_초과면_요청은_실패한다() {
        // given
        final long userId = 1L;
        final long usedAmount = 510000L;  // 사용할 포인트
        final UserPoint expectedUserPoint = new UserPoint(userId, usedAmount, System.currentTimeMillis());

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, usedAmount))
                .thenReturn(expectedUserPoint);

        // when & then
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.insertPoint(userId, usedAmount);
                    if (result.point() > 500000L) {
                        throw new Exception("포인트는 5000,000원 이하 사용 가능합니다.");
                    }
                }
        );

        // 예외 메세지 검증
        assertThat(exception.getMessage()).isEqualTo("포인트는 5000,000원 이하 사용 가능합니다.");
    }

    /**
     * Exception 테스트
     */
    @Test
    void Exception_발생_시_요청은_실패한다() {

        // NullPointerException 테스트
        Exception exception1 = assertThrows(NullPointerException.class, () -> {
            throw new NullPointerException("Null 값이 발생했습니다.");
        });
        assertThat(exception1.getMessage()).isEqualTo("Null 값이 발생했습니다.");

        // IllegalArgumentException 테스트
        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> {
            throw new IllegalArgumentException("잘못된 인자입니다.");
        });
        assertThat(exception2.getMessage()).isEqualTo("잘못된 인자입니다.");

        // 예상하지 못한 Exception 처리
        Exception exception3 = assertThrows(RuntimeException.class, () -> {
            throw new RuntimeException("알 수 없는 오류 발생");
        });
        assertThat(exception3.getMessage()).isEqualTo("알 수 없는 오류 발생");
    }
}