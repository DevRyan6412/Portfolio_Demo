package com.example.demo.repository;

import com.example.demo.domain.entity.IssuePostFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssuePostFileRepository extends JpaRepository<IssuePostFile, Long> {

    // 특정 이슈에 업로드된 파일 목록 조회
    List<IssuePostFile> findByIssueId(Long issueId);

    Optional<IssuePostFile> findByFileName(String fileName);
}





