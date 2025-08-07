package com.example.demo.repository;

import com.example.demo.domain.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    // 기존 메서드
    @Query("SELECT DISTINCT pm.project FROM ProjectMember pm WHERE pm.user.email = :email AND pm.projectRole = :role")
    List<Project> findProjectsByUserAndRole(@Param("email") String email, @Param("role") ProjectRole role);
    boolean existsByProjectAndUser(Project project, User user);
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    // 추가 메서드 - 프로젝트별 멤버 수 카운트
    @Query("SELECT COUNT(pm) FROM ProjectMember pm WHERE pm.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    // 사용자별 참여 프로젝트 수 카운트
    @Query("SELECT COUNT(pm) FROM ProjectMember pm WHERE pm.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    // 프로젝트별 멤버 역할 통계
    @Query("SELECT pm.projectRole, COUNT(pm) FROM ProjectMember pm WHERE pm.project.id = :projectId GROUP BY pm.projectRole")
    List<Object[]> countMemberRolesByProject(@Param("projectId") Long projectId);

    // 멤버 수 상위 프로젝트 조회
    @Query(value = "SELECT p.id, p.project_name, COUNT(pm.user_id) as member_count " +
            "FROM project p " +
            "JOIN project_member pm ON p.id = pm.project_id " +
            "GROUP BY p.id, p.project_name " +
            "ORDER BY member_count DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findTopProjectsByMemberCount(@Param("limit") int limit);
}