package com.example.demo.repository;

import com.example.demo.domain.entity.GlobalNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlobalNoticeRepository extends JpaRepository<GlobalNotice, Long> {
    // 최신 글로벌 공지를 조회하는 메서드
    Optional<GlobalNotice> findTopByOrderByCreatedDateDesc();
}
