package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    // 유저별 락 관리용 Map
    private final ConcurrentHashMap<Long, Lock> locks = new ConcurrentHashMap<>();

    // 특정 유저의 포인트 조회
    public UserPoint selectPoint(long id) {
        return userPointTable.selectById(id);
    }

    // 특정 유저의 포인트 충전/이용 내역 조회
    public List<PointHistory> selectPointHistory(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    // 특정 유저의 포인트 충전
    public UserPoint insertPoint(long id, long amount){
        Lock lock = locks.computeIfAbsent(id, k -> new ReentrantLock(true));
        boolean locked = false;

        try {
            // 락을 시도하고, 3초 이내에 락을 얻지 못하면 예외 발생
            locked = lock.tryLock(3, TimeUnit.SECONDS);

            if(!locked) {
                throw new IllegalStateException("요청이 지연되어 처리에 실패했습니다. 다시 시도해주세요.");
            }

            return userPointTable.insertOrUpdate(id, amount);
        } catch (InterruptedException e) {
            throw new IllegalStateException("포인트 충전 처리 중 문제가 발생했습니다.");
        } finally {
            if(locked) {
                lock.unlock();
            }
        }
    }

    // 특정 유저의 포인트 사용
    public UserPoint updatePoint(long id, long amount){
        Lock lock = locks.computeIfAbsent(id, k -> new ReentrantLock(true));
        boolean locked = false;

        try {
            locked = lock.tryLock(3, TimeUnit.SECONDS);

            if(!locked) {
                throw new IllegalStateException("요청이 지연되어 처리에 실패했습니다. 다시 시도해주세요.");
            }

            return userPointTable.insertOrUpdate(id, amount);
        } catch (InterruptedException e) {
            throw new IllegalStateException("포인트 입력 처리 중 문제가 발생했습니다.");
        }finally {
            if(locked) {
                lock.unlock();
            }
        }
    }
}
