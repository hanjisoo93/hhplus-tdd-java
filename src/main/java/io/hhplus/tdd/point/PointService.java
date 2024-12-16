package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

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
        return userPointTable.insertOrUpdate(id, amount);
    }

    // 특정 유저의 포인트 사용
    public UserPoint updatePoint(long id, long amount){
        return userPointTable.insertOrUpdate(id, amount);
    }
}
