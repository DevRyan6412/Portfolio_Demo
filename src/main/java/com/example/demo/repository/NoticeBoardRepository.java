package com.example.demo.repository;

import com.example.demo.domain.entity.NoticeBoard;
import com.example.demo.domain.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeBoardRepository extends JpaRepository<NoticeBoard, Long> {
    // 특정 프로젝트에 속한 게시글 목록 조회
    List<NoticeBoard> findByProject(Project project);

    // 특정 게시글(id)이 해당 프로젝트에 속하는지 조회
    Optional<NoticeBoard> findByIdAndProject(Long id, Project project);
    // 페이징 처리를 위한 메서드 추가
    Page<NoticeBoard> findByProject(Project project, Pageable pageable);
}
