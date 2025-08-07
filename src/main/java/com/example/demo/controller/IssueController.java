package com.example.demo.controller;

import com.example.demo.domain.dto.IssueCommentDTO;
import com.example.demo.domain.dto.IssueDTO;
import com.example.demo.domain.entity.IssuePostFile;
import com.example.demo.domain.entity.IssueStatus;
import com.example.demo.domain.entity.Project;
import com.example.demo.service.FileUploadService;
import com.example.demo.service.IssueCommentService;
import com.example.demo.service.IssueService;
import com.example.demo.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/projects/{projectId}/board/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;
    private final IssueCommentService issueCommentService;
    private final FileUploadService fileUploadService;
    private final ProjectService projectService;

    /**
     * ✅ 이슈 목록, 검색 및 상태 필터링 페이지
     */
    @GetMapping
    public String getIssueList(@PathVariable("projectId") Long projectId,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "sortBy", required = false, defaultValue = "latest") String sortBy,
                               @RequestParam(value = "status", required = false) IssueStatus status, // ✅ 추가됨
                               @PageableDefault(size = 10) Pageable pageable, // ✅ 페이징 처리 추가
                               Model model) {
        Page<IssueDTO> issuePage;

        if (status != null) { // ✅ 상태별 필터링 적용
            issuePage = issueService.getIssuesByProjectAndStatus(projectId, status, pageable); // ✅ Pageable 추가
            model.addAttribute("issuePaging", issuePage.getContent());
            model.addAttribute("totalPages", issuePage.getTotalPages());
            model.addAttribute("currentPage", issuePage.getNumber());
        } else if (keyword != null && !keyword.isBlank()) {
            issuePage = issueService.getIssuesByProjectAndKeywordSorted(projectId, keyword, sortBy, pageable);
            model.addAttribute("issuePaging", issuePage.getContent());
            model.addAttribute("totalPages", issuePage.getTotalPages());
            model.addAttribute("currentPage", issuePage.getNumber());
        } else {
            issuePage = issueService.getAllIssuesByProjectSorted(projectId, sortBy, pageable);
            model.addAttribute("issuePaging", issuePage.getContent());
            model.addAttribute("totalPages", issuePage.getTotalPages());
            model.addAttribute("currentPage", issuePage.getNumber());
        }

        model.addAttribute("projectId", projectId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("status", status); // ✅ 필터링된 상태 유지

        Project project = projectService.getProjectById(projectId);
        model.addAttribute("project", project);
        return "issuelist";
    }




    /**
     * 이슈 상세 페이지
     * - 이슈 정보와 함께 댓글 목록 표시 (부모 댓글 페이징 + 자식 댓글 모두 표시)
     */
    @GetMapping("/{id}")
    public String getIssueDetail(@PathVariable("projectId") Long projectId,
                                 @PathVariable Long id,
                                 @PageableDefault(size = 5, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable,
                                 Model model) {
        try {
            // 이슈 정보 조회
            IssueDTO issue = issueService.getIssueByIdAndProject(id, projectId);

            // 파일 목록 가져오기
            List<IssuePostFile> files = fileUploadService.getFilesByIssueId(id);
            issue.setFiles(files);

            // 부모 댓글만 페이징 처리하여 가져오기 (최신순)
            Page<IssueCommentDTO> parentCommentPage = issueCommentService.getParentCommentsByIssue(id, pageable);
            List<IssueCommentDTO> parentComments = parentCommentPage.getContent();

            // 부모 댓글 ID 목록 추출
            List<Long> parentIds = parentComments.stream()
                    .map(IssueCommentDTO::getId)
                    .filter(pid -> pid != null)
                    .collect(Collectors.toList());

            // 자식 댓글 조회
            List<IssueCommentDTO> childComments = parentIds.isEmpty()
                    ? List.of()
                    : issueCommentService.getChildCommentsByParentIds(parentIds);

            // 모든 댓글을 하나의 리스트로 합치기
            List<IssueCommentDTO> allComments = new ArrayList<>(parentComments);
            allComments.addAll(childComments);

            // 모델에 데이터 추가
            model.addAttribute("issue", issue);
            model.addAttribute("comments", allComments);
            model.addAttribute("projectId", projectId);
            model.addAttribute("currentPage", pageable.getPageNumber());
            model.addAttribute("totalPages", parentCommentPage.getTotalPages() > 0 ? parentCommentPage.getTotalPages() : 1);

            // 프로젝트 정보 추가
            Project project = projectService.getProjectById(projectId);
            model.addAttribute("project", project);

            return "issuedetail";
        } catch (Exception e) {
            System.err.println("🚨 [ERROR] 이슈 상세 페이지 로드 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/projects/" + projectId + "/board/issues?error=true";
        }
    }

    /**
     * ✅ 이슈 작성 페이지
     */
    @GetMapping("/new")
    public String createIssueForm(@PathVariable("projectId") Long projectId, Model model) {
        model.addAttribute("issue", new IssueDTO());
        model.addAttribute("projectId", projectId);
        Project project = projectService.getProjectById(projectId);
        model.addAttribute("project", project);
        return "issueform";  // Thymeleaf 템플릿 (form.html)
    }

    /**
     * ✅ 파일 업로드
     */
    @PostMapping("/{id}/upload")
    public String uploadFile(@PathVariable("projectId") Long projectId,
                             @PathVariable Long id,
                             @RequestParam("file") MultipartFile file, Model model) {
        try {
            fileUploadService.storeFile(id, file);  //파일 업로드
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류 발생", e);
        }
        Project project = projectService.getProjectById(projectId);
        model.addAttribute("project", project);
        return "redirect:/projects/" + projectId + "/board/issues/" + id;
    }

    /**
     * ✅ 이슈 등록 처리
     */
    @PostMapping
    public String createIssue(@PathVariable("projectId") Long projectId,
                              @ModelAttribute IssueDTO issueDTO,
                              @RequestParam(value = "file", required = false) MultipartFile file, Model model) {
        if (file != null && !file.isEmpty()) {
            issueService.createIssue(issueDTO, projectId, file);  // ✅ 파일이 있는 경우
        } else {
            issueService.createIssue(issueDTO, projectId);  // ✅ 파일 없이 이슈 등록
        }

        Project project = projectService.getProjectById(projectId);
        model.addAttribute("project", project);

        return "redirect:/projects/" + projectId + "/board/issues";
    }

    /**
     * ✅ 이슈 수정 페이지
     */
    @GetMapping("/edit/{id}")
    public String editIssueForm(@PathVariable("projectId") Long projectId,
                                @PathVariable Long id, Model model) {
        IssueDTO issue = issueService.getIssueByIdAndProject(id, projectId);

        // 기존 첨부 파일 리스트 추가
        List<IssuePostFile> files = fileUploadService.getFilesByIssueId(id);
        issue.setFiles(files);

        model.addAttribute("issue", issue);
        model.addAttribute("projectId", projectId);
        Project project = projectService.getProjectById(projectId);
        model.addAttribute("project", project);
        return "issueeditform";
    }

    /**
     * ✅ 이슈 수정 처리
     */
    @PostMapping("/update/{id}")
    public String updateIssue(@PathVariable("projectId") Long projectId,
                              @PathVariable Long id,
                              @ModelAttribute IssueDTO issueDTO,
                              @RequestParam(value = "newFiles", required = false) MultipartFile[] newFiles, Model model) {
        issueService.updateIssue(id, issueDTO, projectId, newFiles); // ✅ 수정된 메서드 호출
        Project project = projectService.getProjectById(projectId);
        model.addAttribute("project", project);
        return "redirect:/projects/" + projectId + "/board/issues";
    }

    /**
     * ✅ 이슈 삭제
     */
    @PostMapping("/delete/{id}")
    public String deleteIssue(@PathVariable("projectId") Long projectId,
                              @PathVariable Long id, Model model) {
        issueService.deleteIssue(id, projectId);
        Project project = projectService.getProjectById(projectId);
        model.addAttribute("project", project);
        return "redirect:/projects/" + projectId + "/board/issues";
    }
}