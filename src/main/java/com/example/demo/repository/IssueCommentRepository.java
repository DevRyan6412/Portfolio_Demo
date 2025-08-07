package com.example.demo.repository;

import com.example.demo.domain.entity.Issue;
import com.example.demo.domain.entity.IssueComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueCommentRepository extends JpaRepository<IssueComment, Long> {

    // 특정 이슈의 모든 댓글 조회 (생성일 오름차순)
    List<IssueComment> findByIssueOrderByCreatedDateAsc(Issue issue);

    // 특정 이슈의 모든 댓글 페이징 조회 (생성일 오름차순)
    Page<IssueComment> findByIssueOrderByCreatedDateAsc(Issue issue, Pageable pageable);

    // 부모 댓글만 페이징 조회 (parentComment가 null인 경우, 최신순)
    Page<IssueComment> findByIssueAndParentCommentIsNullOrderByCreatedDateDesc(Issue issue, Pageable pageable);

    // 특정 부모 댓글 ID 목록에 해당하는 자식 댓글 조회 (오래된 순)
    @Query("SELECT c FROM IssueComment c WHERE c.parentComment.id IN :parentIds ORDER BY c.createdDate ASC")
    List<IssueComment> findByParentCommentIds(@Param("parentIds") List<Long> parentIds);
}
