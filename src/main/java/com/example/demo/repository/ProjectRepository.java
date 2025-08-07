package com.example.demo.repository;

import com.example.demo.domain.entity.Project;
import com.example.demo.domain.entity.ProjectRole;
import com.example.demo.domain.entity.ProjectStatus;
import com.example.demo.domain.entity.User;
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
public interface ProjectRepository extends JpaRepository<Project, Long> {
    // 기존 메서드
    List<Project> findByProjectMembersUser(User user);
    Optional<Project> findById(Long id);
    List<Project> findByCreator(User creator);

    // 추가 메서드 - 상태별 프로젝트 조회
    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

    // 상태별 프로젝트 카운트
    long countByStatus(ProjectStatus status);

    // 최신 프로젝트 조회
    List<Project> findTop5ByOrderByStartDateDesc();

    // 프로젝트명으로 검색
    Page<Project> findByProjectNameContaining(String keyword, Pageable pageable);

    // 프로젝트명 + 상태로 검색
    Page<Project> findByProjectNameContainingAndStatus(String keyword, ProjectStatus status, Pageable pageable);

    // 각 상태별 프로젝트 수 통계
    @Query("SELECT p.status, COUNT(p) FROM Project p GROUP BY p.status")
    List<Object[]> countProjectsByStatus();

    // 기간별 생성된 프로젝트 수
    @Query("SELECT FUNCTION('DATE', p.startDate) as date, COUNT(p) " +
            "FROM Project p " +
            "WHERE p.startDate >= :startDate " +
            "GROUP BY FUNCTION('DATE', p.startDate) " +
            "ORDER BY date")
    List<Object[]> countProjectsCreatedAfter(@Param("startDate") LocalDateTime startDate);
}