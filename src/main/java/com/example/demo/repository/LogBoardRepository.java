package com.example.demo.repository;

import com.example.demo.domain.entity.LogBoard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogBoardRepository extends JpaRepository<LogBoard, Long> {
    // 기본 조회 메서드 (유지)
    Page<LogBoard> findByProjectIdOrderByActionDateDesc(Long projectId, Pageable pageable);
    List<LogBoard> findTop10ByOrderByActionDateDesc();
    Page<LogBoard> findByActionDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<LogBoard> findByBoardNm(String boardNm, Pageable pageable);
    Page<LogBoard> findByAction(String action, Pageable pageable);
    Page<LogBoard> findByBoardNmAndAction(String boardNm, String action, Pageable pageable);
    Page<LogBoard> findByBoardNmAndActionDateBetween(String boardNm, LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<LogBoard> findByActionAndActionDateBetween(String action, LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<LogBoard> findByBoardNmAndActionAndActionDateBetween(
            String boardNm,
            String action,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    // 기본 카운트 메서드 (유지)
    long countByActionDateAfter(LocalDateTime date);

    // TOP 6 통계를 위한 메서드

    // 1. 시간대별 활동량 통계 (추가)
    @Query("SELECT FUNCTION('HOUR', l.actionDate) as hour, COUNT(l) " +
            "FROM LogBoard l " +
            "WHERE l.actionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('HOUR', l.actionDate) " +
            "ORDER BY hour")
    List<Object[]> countHourlyActivities(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    // 2. 게시판별 활동 비율 (기존 유지)
    @Query("SELECT l.boardNm, COUNT(l) FROM LogBoard l " +
            "WHERE l.actionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY l.boardNm")
    List<Object[]> countByBoardNmGroupBy(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    // 옵션: 모든 기간 게시판 통계 (기존 유지)
    @Query("SELECT l.boardNm, COUNT(l) FROM LogBoard l GROUP BY l.boardNm")
    List<Object[]> countByBoardNmGroupBy();

    // 3. 활동 유형별 통계 (기존 유지)
    @Query("SELECT l.action, COUNT(l) FROM LogBoard l " +
            "WHERE l.actionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY l.action")
    List<Object[]> countByActionGroupBy(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // 4. 가장 활동적인 사용자 목록 (추가)
    @Query("SELECT l.cName, COUNT(l) as cnt " +
            "FROM LogBoard l " +
            "WHERE l.actionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY l.cName " +
            "ORDER BY cnt DESC")
    List<Object[]> findTopActiveUsers(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      Pageable pageable);

    // 5. 프로젝트별 활동 통계 (추가)
    @Query("SELECT l.project.id, l.project.projectName, COUNT(l) " +
            "FROM LogBoard l " +
            "WHERE l.project IS NOT NULL AND l.actionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY l.project.id, l.project.projectName " +
            "ORDER BY COUNT(l) DESC")
    List<Object[]> countActivitiesByProject(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            Pageable pageable);

    // 6. 일별 활동 통계 (기존 유지 및 수정)
    @Query("SELECT FUNCTION('DATE', l.actionDate) as date, COUNT(l) " +
            "FROM LogBoard l " +
            "WHERE l.actionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('DATE', l.actionDate) " +
            "ORDER BY date")
    List<Object[]> countDailyActivities(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // 기존 메서드 유지 (필요시)
    @Query("SELECT FUNCTION('DATE', l.actionDate) as date, COUNT(l) " +
            "FROM LogBoard l " +
            "WHERE l.actionDate >= :startDate " +
            "GROUP BY FUNCTION('DATE', l.actionDate) " +
            "ORDER BY date")
    List<Object[]> countDailyActivitiesAfter(@Param("startDate") LocalDateTime startDate);

    // 게시판과 활동 유형별 통계
    @Query("SELECT l.boardNm, l.action, COUNT(l) " +
            "FROM LogBoard l " +
            "WHERE l.actionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY l.boardNm, l.action " +
            "ORDER BY l.boardNm, l.action")
    List<Object[]> countActivitiesByBoardAndAction(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    // Add to LogBoardRepository interface
    @Query("SELECT lb.action, COUNT(lb) FROM LogBoard lb WHERE lb.boardNm = :boardNm AND lb.actionDate BETWEEN :startDateTime AND :endDateTime GROUP BY lb.action")
    List<Object[]> countByBoardNmAndActionGroupBy(String boardNm, LocalDateTime startDateTime, LocalDateTime endDateTime);

    // Also, you might need this method for the second implementation
    @Query("SELECT COUNT(lb) FROM LogBoard lb WHERE lb.action = :action AND lb.actionDate BETWEEN :startDateTime AND :endDateTime")
    Long countByActionAndActionDateBetween(String action, LocalDateTime startDateTime, LocalDateTime endDateTime);

    @Query("SELECT CAST(DATE(lb.actionDate) AS date), lb.action, COUNT(lb) FROM LogBoard lb WHERE lb.actionDate BETWEEN :startDateTime AND :endDateTime GROUP BY CAST(DATE(lb.actionDate) AS date), lb.action ORDER BY CAST(DATE(lb.actionDate) AS date)")
    List<Object[]> countDailyActivitiesByActionType(LocalDateTime startDateTime, LocalDateTime endDateTime);
}