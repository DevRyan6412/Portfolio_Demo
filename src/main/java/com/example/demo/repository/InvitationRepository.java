package com.example.demo.repository;

import com.example.demo.domain.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    // 토큰으로 Invitation 엔티티를 조회하는 메서드
    Optional<Invitation> findByToken(String token);
}