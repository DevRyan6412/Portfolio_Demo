package com.example.demo.service;

import com.example.demo.domain.dto.IssueCommentDTO;
import com.example.demo.domain.entity.Issue;
import com.example.demo.domain.entity.IssueComment;
import com.example.demo.domain.entity.Project;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.IssueCommentRepository;
import com.example.demo.repository.IssueRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssueCommentService {

    private final IssueCommentRepository issueCommentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final LogBoardService logBoardService; // ✅ LogBoardService 의존성 추가

    /**
     * 특정 이슈의 모든 댓글(대댓글 포함) 조회
     */
    @Transactional(readOnly = true)
    public List<IssueCommentDTO> getCommentsByIssue(Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("이슈를 찾을 수 없습니다."));
        return issueCommentRepository.findByIssueOrderByCreatedDateAsc(issue).stream()
                .map(IssueCommentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 부모 댓글만 페이징 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public Page<IssueCommentDTO> getParentCommentsByIssue(Long issueId, Pageable pageable) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("이슈를 찾을 수 없습니다."));
        return issueCommentRepository.findByIssueAndParentCommentIsNullOrderByCreatedDateDesc(issue, pageable)
                .map(IssueCommentDTO::fromEntity);
    }

    /**
     * 특정 부모 댓글들의 모든 자식 댓글 조회 (오래된 순)
     */
    @Transactional(readOnly = true)
    public List<IssueCommentDTO> getChildCommentsByParentIds(List<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<IssueComment> childComments = issueCommentRepository.findByParentCommentIds(parentIds);
        return childComments.stream()
                .map(IssueCommentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 댓글 작성 (부모 댓글)
     */
    @Transactional
    public IssueCommentDTO createComment(Long issueId, String userEmail, String contents, Long projectId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("이슈를 찾을 수 없습니다."));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다."));
        IssueComment comment = IssueComment.builder()
                .issue(issue)
                .user(user)
                .contents(contents)
                .parentComment(null)
                .build();
        comment.setProject(project);
        comment.setBoardNm("comment");
        comment.setCName(user.getName());
        comment.setCreatedBy(userEmail);
        comment.setCreatedDate(LocalDateTime.now());
        // ✅ 이슈 삭제 로그 추가 (삭제 전에 로그를 남겨야 함)
        logBoardService.saveLog(
                issue.getProject(),
                "comment",  // 게시판 이름, 이슈 서비스에서는 "Issue"로 통일
                issue.getId(),
                issue.getCreatedBy(),
                issue.getModifiedBy(),
                "ADD",
                LocalDateTime.now(),
                issue.getCName()
        );
        IssueComment savedComment = issueCommentRepository.save(comment);
        return IssueCommentDTO.fromEntity(savedComment);
    }

    /**
     * 대댓글 작성 (자식 댓글)
     */
    @Transactional
    public IssueCommentDTO createReply(Long issueId, Long userId, Long parentCommentId, String contents) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("이슈를 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        IssueComment parentComment = issueCommentRepository.findById(parentCommentId)
                .orElseThrow(() -> new RuntimeException("부모 댓글을 찾을 수 없습니다."));
        Project project = issue.getProject();
        IssueComment reply = IssueComment.builder()
                .issue(issue)
                .user(user)
                .parentComment(parentComment)
                .contents(contents)
                .build();
        reply.setProject(project);
        reply.setBoardNm("reply");
        reply.setCName(user.getName());
        reply.setCreatedBy(user.getEmail());
        reply.setCreatedDate(LocalDateTime.now());
        // ✅ 이슈 삭제 로그 추가 (삭제 전에 로그를 남겨야 함)
        logBoardService.saveLog(
                issue.getProject(),
                "reply",  // 게시판 이름, 이슈 서비스에서는 "Issue"로 통일
                issue.getId(),
                issue.getCreatedBy(),
                issue.getModifiedBy(),
                "ADD",
                LocalDateTime.now(),
                issue.getCName()
        );
        IssueComment savedReply = issueCommentRepository.save(reply);
        if (parentComment.getReplies() != null) {
            parentComment.getReplies().add(savedReply);
        }
        return IssueCommentDTO.fromEntity(savedReply);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public IssueCommentDTO updateComment(Long commentId, String newContents) {
        IssueComment comment = issueCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        comment.setContents(newContents);
        return IssueCommentDTO.fromEntity(issueCommentRepository.save(comment));
    }

    /**
     * 댓글 삭제 (대댓글도 함께 삭제)
     */
    @Transactional
    public void deleteComment(Long commentId) {
        IssueComment comment = issueCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        issueCommentRepository.delete(comment);
    }
}
