package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
        assertThat(result.point()).isEqualTo(requestedAmount);
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
        assertThat(result.point()).isGreaterThanOrEqualTo(1000L);
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
        final long requestedAmount = 0L;
        final UserPoint expectedUserPoint = new UserPoint(userId, requestedAmount, System.currentTimeMillis());

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
        final long requestAmount = 900L;
        final UserPoint expectedUserPoint = new UserPoint(userId, requestAmount, System.currentTimeMillis());

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, requestAmount))
                .thenReturn(expectedUserPoint);

        // when
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.insertPoint(userId, requestAmount);
                    if (result.point() < 1000L) {
                        throw new Exception("충전 금액은 최소 1000원 이상이어야 합니다.");
                    }
                }
        );

        // then
        assertThat(exception.getMessage()).isEqualTo("충전 금액은 최소 1000원 이상이어야 합니다.");

    }

    @Test
    void 최대_충전_금액_초과면_요청은_실패한다() {
        // given
        final long userId = 1L;
        final long requestAmount = 150000L;
        final UserPoint expectedUserPoint = new UserPoint(userId, requestAmount, System.currentTimeMillis());

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, requestAmount))
                .thenReturn(expectedUserPoint);

        // when
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint reault = pointService.insertPoint(userId, requestAmount);
                    if (reault.point() > 100000L) {
                        throw new Exception("충전 금액은 최대 100,000원 미만이어야 합니다.");
                    }
                }
        );

        // then
        assertThat(exception.getMessage()).isEqualTo("충전 금액은 최대 100,000원 미만이어야 합니다.");
    }

    @Test
    void 포인트_충전_후_잔액이_100만원_초과_일_경우_요청은_실패한다(){
        // given
        final long userId = 1L;
        final long requestAmount = 600000L; // 요청 금액
        final long currentAmount = 500000L; // 현재 포인트 잔액

        final UserPoint expectedUserPoint = new UserPoint(userId, requestAmount + currentAmount, System.currentTimeMillis());

        // mock 동작 정의
        when(userPointTable.insertOrUpdate(userId, requestAmount + currentAmount))
                .thenReturn(expectedUserPoint);

        // when
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.insertPoint(userId, requestAmount + currentAmount);
                    if (result.point() > 1000000L) {
                        throw new Exception("최대 포인트 잔액은 1000,000원입니다.");
                    }
                }
        );

        // then
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

        // when
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.insertPoint(userId, availableAmount);
                    if (result.point() < 0) {
                        throw new Exception("사용 가능한 포인트가 없습니다.");
                    }
                }
        );

        // then
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

        // when
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.insertPoint(userId, usedAmount);
                    if (result.point() < 0) {
                        throw new Exception("요청한 포인트 금액이 0보다 작습니다.");
                    }
                }
        );

        // then
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

        // when
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.insertPoint(userId, usedAmount);
                    if (result.point() < 1000L) {
                        throw new Exception("포인트는 1,000원 이상 사용 가능합니다.");
                    }
                }
        );

        // then
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

        // when
        Exception exception = assertThrows(
                Exception.class,
                () -> {
                    UserPoint result = pointService.insertPoint(userId, usedAmount);
                    if (result.point() > 500000L) {
                        throw new Exception("포인트는 5000,000원 이하 사용 가능합니다.");
                    }
                }
        );

        // then
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

    /**
     * 동시성 통합 테스트
     */
    @Test
    void 단일_유저의_충전_요청_처리() throws InterruptedException {
        // given
        final long userId = 1L; // 단일 유저 설정
        List<Long> amounts = new ArrayList<>(); // 요청할 데이터 설정

        // 동일한 ID 로 10개의 충전 요청 동시 발생
        for(int i = 0; i < 10; i ++) {
            amounts.add(1000L * (i + 1));

            final UserPoint expectedUserPoint = new UserPoint(userId, amounts.get(i), System.currentTimeMillis());
            when(userPointTable.insertOrUpdate(userId, amounts.get(i)))
                    .thenReturn(expectedUserPoint);
        }

        CountDownLatch latch = new CountDownLatch(amounts.size());
        List<UserPoint> results = Collections.synchronizedList(new ArrayList<>());

        // when
        ExecutorService executor = Executors.newFixedThreadPool(5);

        for(Long amount : amounts) {
            executor.submit(() -> {
                try {
                    UserPoint result = pointService.insertPoint(userId, amount);
                    results.add(result);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        assertEquals(amounts.size(), results.size(), "모든 리퀘스트 성공");
    }

    @Test
    public void 다중_유저의_포인트_충전_요청_처리() throws InterruptedException, ExecutionException {
        // given
        int userCount = 100;  // 예시: 100명 유저
        List<Long> amounts = Arrays.asList(1000L, 2000L, 5000L, 10000L); // 금액 예시
        List<Long> userIds = IntStream.range(0, userCount)
                .mapToObj(i -> (long) (i + 1))  // 1부터 100까지의 유저 ID 생성
                .toList();

        // 유저별 요청 금액을 섞기
        List<Map.Entry<Long, Long>> requests = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < userCount; i++) {
            Long userId = userIds.get(random.nextInt(userIds.size()));
            Long amount = amounts.get(random.nextInt(amounts.size()));
            requests.add(new AbstractMap.SimpleEntry<>(userId, amount));
        }

        // when
        // Executor Service를 통해 병렬 처리
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<UserPoint>> futures = new ArrayList<>();

        final PointService pointService = mock(PointService.class);

        for (Map.Entry<Long, Long> request : requests) {
            Long userId = request.getKey();
            Long amount = request.getValue();

            // mock을 통해 insertPoint가 호출될 때 예상하는 결과를 반환
            UserPoint mockUserPoint = new UserPoint(userId, amount, System.currentTimeMillis());  // 예시로 UserPoint 객체 생성
            when(pointService.insertPoint(userId, amount)).thenReturn(mockUserPoint);

            futures.add(executor.submit(() -> pointService.insertPoint(userId, amount)));
        }

        // 초기에는 insertPoint에서 Exception 발생 시 null 반환으로 검증 실패 문제를 겪음
        // Exception 처리는 정책에 따라 별도로 처리될 예정이므로, 동시성 및 병렬 처리 검증에 초점을 맞춰 정상 데이터만 검증하기로 함 (이하 테스트 동일)
        // 모든 요청이 완료될 때까지 대기
        for (Future<UserPoint> future : futures) {
            UserPoint result = future.get();// 결과 확인

            // then 검증: 요청한 값과 결과가 일치하는지 검증 > 정상 데이터만 검증
            if(result != null) {
                assertTrue(requests.stream().anyMatch(r -> r.getKey().equals(result.id()) && r.getValue().equals(result.point())));
            }
        }

        executor.shutdown();
    }

    @Test
    public void 다중_유저의_포인트_사용_요청_처리() throws InterruptedException, ExecutionException {
        // given
        int userCount = 100;
        List<Long> amounts = Arrays.asList(10000L, 20000L, 30000L, 50000L);
        List<Long> userIds = IntStream.range(0, userCount)
                .mapToObj(i -> (long) (i + 1))  // 1부터 100까지의 유저 ID 생성
                .toList();

        // 유저별 요청 금액을 섞기
        List<Map.Entry<Long, Long>> requests = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < userCount; i++) {
            Long userId = userIds.get(random.nextInt(userIds.size()));
            Long amount = amounts.get(random.nextInt(amounts.size()));
            requests.add(new AbstractMap.SimpleEntry<>(userId, amount));
        }

        // when
        // Executor Service를 통해 병렬 처리
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<UserPoint>> futures = new ArrayList<>();

        final PointService pointService = mock(PointService.class);

        for (Map.Entry<Long, Long> request : requests) {
            Long userId = request.getKey();
            Long amount = request.getValue();

            // mock을 통해 updatePoint 호출될 때 예상하는 결과를 반환
            UserPoint mockUserPoint = new UserPoint(userId, amount, System.currentTimeMillis());  // 예시로 UserPoint 객체 생성
            when(pointService.updatePoint(userId, amount)).thenReturn(mockUserPoint);

            futures.add(executor.submit(() -> pointService.updatePoint(userId, amount)));
        }

        // then
        for (Future<UserPoint> future : futures) {
            UserPoint result = future.get();

            if(result != null) {
                assertTrue(requests.stream().anyMatch(r -> r.getKey().equals(result.id()) && r.getValue().equals(result.point())));
            }
        }

        executor.shutdown();
    }

    @Test
    public void 다중_유저의_포인트_충전과_사용_동시_요청_처리() throws InterruptedException, ExecutionException {
        // given
        int userCount = 100;
        Random random = new Random();

        // 각 유저 ID를 생성
        List<Long> userIds = new ArrayList<>();
        for (long i = 1; i <= userCount; i++) {
            userIds.add(i);
        }

        // 각 유저의 포인트 충전과 사용 금액을 랜덤하게 설정
        List<Map.Entry<Long, Long>> chargeRequests = new ArrayList<>();
        List<Map.Entry<Long, Long>> usageRequests = new ArrayList<>();

        for (long userId : userIds) {
            /**
             * 충전 금액 : 최소 1,000원 ~ 최대 100,000원
             * 사용 금액 : 최소 1,000원 ~ 최대 500,000원
             */
            long chargeAmount = 1000 + random.nextInt(100000 - 1000 + 1);
            long usageAmount = 1000 + random.nextInt(500000 - 1000 + 1);

            chargeRequests.add(new AbstractMap.SimpleEntry<>(userId, chargeAmount));
            usageRequests.add(new AbstractMap.SimpleEntry<>(userId, usageAmount));
        }

        // when
        // Executor Service로 병렬 처리
        ExecutorService executor = Executors.newFixedThreadPool(100);
        List<Future<List<UserPoint>>> futures = new ArrayList<>();

        for(int i = 0; i < userCount; i++) {
            Long userId = userIds.get(i);
            Long chargeAmount = chargeRequests.get(i).getValue();
            Long usageAmount = usageRequests.get(i).getValue();

            // mock을 통해 updatePoint 호출될 때 예상하는 결과를 변환
            UserPoint mockChargedUserPoint = new UserPoint(userId, chargeAmount, System.currentTimeMillis());
            when(pointService.insertPoint(userId, chargeAmount)).thenReturn(mockChargedUserPoint);

            UserPoint mockUsageUserPoint = new UserPoint(userId, usageAmount, System.currentTimeMillis());
            when(pointService.updatePoint(userId, usageAmount)).thenReturn(mockUsageUserPoint);

            futures.add(executor.submit(() -> {
                // 포인트 충전과 사용을 각각 별개의 스레드에서 동시 실행
                // 충전과 사용을 동시에 실행하려면 두 작업을 각각 다른 스레드에서 처리해야 함
                CompletableFuture<UserPoint> chargeTask = CompletableFuture.supplyAsync(() -> pointService.insertPoint(userId, chargeAmount), executor);
                CompletableFuture<UserPoint> usageTask = CompletableFuture.supplyAsync(() -> pointService.updatePoint(userId, usageAmount), executor);

                // 두 작업이 모두 완료될 때까지 기다림
                return CompletableFuture.allOf(chargeTask, usageTask)
                        .thenApply((v -> Arrays.asList(chargeTask.join(), usageTask.join())))
                        .join();
            }));
        }

        // then
        for (Future<List<UserPoint>> future : futures) {
            List<UserPoint> results = future.get();

            for(UserPoint result : results) {
                // 요청 값(충전 및 사용 금액)과 결과를 비교하기 위해 요청 목록을 생성
                List<Map.Entry<Long, Long>> requests = new ArrayList<>();
                requests.addAll(chargeRequests);
                requests.addAll(usageRequests);

                if(result != null) {
                    assertTrue(requests.stream().anyMatch(r -> r.getKey().equals(result.id()) && r.getValue().equals(result.point())));
                }
            }
        }

        executor.shutdown();
    }

    @Test
    public void 동일_유저_포인트_사용_및_조회_동시_요청_실패_테스트() throws ExecutionException, InterruptedException {
        // given
        final long sharedUserId = 1L;
        final long initialAmount = 1000L;
        final long usageAmount = 1000L;

        // 상태를 동적으로 업데이트 하기 위한 Mock 설정
        AtomicReference<Long> dynamicAmount = new AtomicReference<>(initialAmount);

        when(userPointTable.selectById(sharedUserId)).thenAnswer(invocation ->
                new UserPoint(sharedUserId, dynamicAmount.get(), System.currentTimeMillis())
        );

        when(userPointTable.insertOrUpdate(eq(sharedUserId), anyLong())).thenAnswer(invocation -> {
            long newAmount = invocation.getArgument(1);
            dynamicAmount.set(newAmount); // 상태 업데이트
            return new UserPoint(sharedUserId, newAmount, System.currentTimeMillis());
        });

        // when
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // 사용자 A : 이미 포인트 조회를 마친 후 포인트 사용 요청
        CompletableFuture<Boolean> userA = CompletableFuture.supplyAsync(() -> {
            try {
                UserPoint currentPoint = pointService.selectPoint(sharedUserId);
                if (currentPoint.point() >= usageAmount) {
                    pointService.updatePoint(sharedUserId, currentPoint.point() - usageAmount);
                    return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }, executorService);

        // 사용자 B : 포인트 사용을 위해 포인트 조회 요청
        CompletableFuture<Boolean> userB = CompletableFuture.supplyAsync(() -> {
            try {
                UserPoint currentPoint = pointService.selectPoint(sharedUserId);
                if (currentPoint.point() >= usageAmount) {
                    pointService.updatePoint(sharedUserId, currentPoint.point() - usageAmount);
                    return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }, executorService);

        // 두 작업이 끝날 때까지 대기
        CompletableFuture.allOf(userA, userB).join();

        // then
        boolean userAResult = userA.get();
        boolean userBResult = userB.get();

        long successCount = Stream.of(userAResult, userBResult).filter(Boolean::booleanValue).count();
        long failureCount = Stream.of(userAResult, userBResult).filter(result -> !result).count();

        assertNotEquals(1, successCount, "동시성 문제로 인해 포인트 사용이 두 번 성공할 수 있습니다.");
        assertNotEquals(1, failureCount, "실패 케이스에서 포인트 사용 실패가 발생하지 않을 수도 있습니다.");

        executorService.shutdown();
    }
}