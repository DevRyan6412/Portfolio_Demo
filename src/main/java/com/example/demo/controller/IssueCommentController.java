package com.example.demo.controller;

import com.example.demo.domain.dto.IssueCommentDTO;
import com.example.demo.domain.dto.IssueDTO;
import com.example.demo.domain.entity.Project;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.IssueCommentService;
import com.example.demo.service.IssueService;
import com.example.demo.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/projects/{projectId}/board/issues/{issueId}/comments")
@RequiredArgsConstructor
public class IssueCommentController {

    private final IssueCommentService issueCommentService;
    private final IssueService issueService;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    /**
     * ✅ 댓글 목록 조회 (JSON, 기존 코드 유지)
     */
    @GetMapping("/json")
    @ResponseBody
    public List<IssueCommentDTO> getCommentsJson(@PathVariable Long projectId, @PathVariable Long issueId) {
        return issueCommentService.getCommentsByIssue(issueId);
    }

    /**
     * ✅ 댓글 작성 (기존 코드 유지)
     */
    @PostMapping
    public String createComment(@PathVariable Long projectId,
                                @PathVariable Long issueId,
                                @RequestParam String userEmail,
                                @RequestParam String content,
                                Authentication authentication) {
        String email = getUserEmail(authentication, userEmail);
        issueCommentService.createComment(issueId, email, content, projectId);
        // 이슈 상세 페이지로 리다이렉트 (comments 경로 제외)
        return "redirect:/projects/" + projectId + "/board/issues/" + issueId;
    }

    /**
     * ✅ 대댓글 작성 (기존 코드 유지)
     */
    @PostMapping("/{parentCommentId}/reply")
    public String createReply(@PathVariable Long projectId,
                              @PathVariable Long issueId,
                              @PathVariable Long parentCommentId,
                              Authentication authentication,
                              @RequestParam String content) {
        String userEmail = getUserEmail(authentication, null);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        issueCommentService.createReply(issueId, user.getId(), parentCommentId, content);

        // 이슈 상세 페이지로 리다이렉트 (comments 경로 제외)
        return "redirect:/projects/" + projectId + "/board/issues/" + issueId;
    }

    /**
     * ✅ 댓글 수정 (기존 코드 유지)
     */
    @PostMapping("/update/{commentId}")
    public String updateComment(@PathVariable Long projectId,
                                @PathVariable Long issueId,
                                @PathVariable Long commentId,
                                @RequestParam String newContent) {
        issueCommentService.updateComment(commentId, newContent);
        // 이슈 상세 페이지로 리다이렉트 (comments 경로 제외)
        return "redirect:/projects/" + projectId + "/board/issues/" + issueId;
    }

    /**
     * ✅ 댓글 삭제 (기존 코드 유지)
     */
    @PostMapping("/delete/{commentId}")
    public String deleteComment(@PathVariable Long projectId,
                                @PathVariable Long issueId,
                                @PathVariable Long commentId) {
        issueCommentService.deleteComment(commentId);
        // 이슈 상세 페이지로 리다이렉트 (comments 경로 제외)
        return "redirect:/projects/" + projectId + "/board/issues/" + issueId;
    }

    // ✅ 사용자 이메일 추출 (기존 코드 유지)
    private String getUserEmail(Authentication authentication, String formEmail) {
        if (authentication == null) return formEmail;
        if (authentication.getPrincipal() instanceof OAuth2User) {
            return ((OAuth2User) authentication.getPrincipal()).getAttribute("email");
        }
        if (authentication.getPrincipal() instanceof UserDetails) {
            return authentication.getName();
        }
        return formEmail;
    }
}