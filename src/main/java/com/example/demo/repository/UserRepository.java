package com.example.demo.repository;

import com.example.demo.domain.entity.Role;
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
public interface UserRepository extends JpaRepository<User, Long> {
    // 기존 메서드
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(String email, String name);

    // 추가 메서드 - 최근 가입자 조회
    List<User> findTop5ByOrderByCreatedAtDesc();

    // 활동이 있는 사용자 찾기
    @Query("SELECT DISTINCT l.user FROM LogBoard l WHERE l.actionDate >= :since")
    List<User> findUsersWithActivitySince(@Param("since") LocalDateTime since);

    // 각 사용자별 활동 수 카운트 (상위 N명)
    @Query(value = "SELECT u.id, u.name, u.email, COUNT(l.log_no) as activity_count " +
            "FROM users u " +
            "LEFT JOIN log_board l ON u.id = l.user_id " +
            "WHERE l.action_date >= :since " +
            "GROUP BY u.id, u.name, u.email " +
            "ORDER BY activity_count DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findTopActiveUsers(@Param("since") LocalDateTime since, @Param("limit") int limit);

    // 사용자 이름/이메일로 검색
    Page<User> findByNameContainingOrEmailContaining(String name, String email, Pageable pageable);

    // 역할별 사용자 카운트
    long countByRole(Role role);

    // 역할별 사용자 조회
    Page<User> findByRole(Role role, Pageable pageable);

    // 최근 가입자 조회 (페이징)
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
}