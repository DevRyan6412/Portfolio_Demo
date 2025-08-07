package com.example.demo.repository;

import com.example.demo.domain.entity.CalendarEvent;
import com.example.demo.domain.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    // 기존 메서드
    List<CalendarEvent> findByProjectAndStartDateBetween(Project project, LocalDateTime start, LocalDateTime end);
    Optional<CalendarEvent> findByIdAndProject(Long id, Project project);

    // 추가 메서드 - 프로젝트별 일정 카운트
    long countByProject(Project project);

    // 기간별 일정 카운트
    long countByStartDateBetween(LocalDateTime start, LocalDateTime end);

    // 최근 일정 조회
    List<CalendarEvent> findTop5ByOrderByStartDateDesc();

    // 프로젝트별 일정 조회 (페이징)
    Page<CalendarEvent> findByProject(Project project, Pageable pageable);

    // 다가오는 일정 조회
    List<CalendarEvent> findByStartDateGreaterThanEqualOrderByStartDateAsc(LocalDateTime now);

    // 월별 일정 수 집계 (통계용)
    @Query("SELECT FUNCTION('YEAR', e.startDate) as year, " +
            "FUNCTION('MONTH', e.startDate) as month, " +
            "COUNT(e) FROM CalendarEvent e " +
            "WHERE e.startDate >= :startDate " +
            "GROUP BY FUNCTION('YEAR', e.startDate), FUNCTION('MONTH', e.startDate) " +
            "ORDER BY year, month")
    List<Object[]> countMonthlyEvents(@Param("startDate") LocalDateTime startDate);
}