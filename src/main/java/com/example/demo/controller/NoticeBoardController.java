package com.example.demo.controller;

import com.example.demo.domain.dto.GlobalNoticeDTO;
import com.example.demo.domain.dto.NoticeBoardDTO;
import com.example.demo.domain.entity.*;
import com.example.demo.service.LogBoardService;
import com.example.demo.service.NoticeBoardService;
import com.example.demo.service.ProjectService;
import com.example.demo.service.admin.GlobalNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/projects/{projectId}/board/noticeboard")
@RequiredArgsConstructor
public class NoticeBoardController {

    private static final Logger log = LoggerFactory.getLogger(NoticeBoardController.class);
    private final NoticeBoardService noticeBoardService;
    private final LogBoardService logBoardService;
    private final ProjectService projectService;
    private final GlobalNoticeService globalNoticeService;

//    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
//    @GetMapping
//    public String listNoticeBoards(@PathVariable("projectId") Long projectId,
//                                   @RequestParam(defaultValue = "0") int page,
//                                   @RequestParam(defaultValue = "10") int size,
//                                   Model model,
//                                   Authentication authentication) {
//        // 페이징 처리된 게시글 목록 조회
//        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
//        Page<NoticeBoardDTO> noticeBoardPage = noticeBoardService.getNoticeBoardsByProjectPaging(projectId, pageable);
//        model.addAttribute("noticeBoards", noticeBoardPage.getContent());
//        model.addAttribute("currentPage", page);
//        model.addAttribute("totalPages", noticeBoardPage.getTotalPages());
//        model.addAttribute("projectId", projectId);
//
//        // 프로젝트 객체 조회 및 모델에 추가 (예: navbar에서 사용)
//        Project project = projectService.getProjectById(projectId);
//        model.addAttribute("project", project);
//
//        // 현재 인증된 사용자의 이메일(또는 ID) 추출
//        String email;
//        if (authentication.getPrincipal() instanceof OAuth2User) {
//            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
//            email = (String) oauth2User.getAttribute("email");
//            log.info("OAuth2 로그인 사용자 이메일: {}", email);
//        } else {
//            email = authentication.getName();
//            log.info("일반 로그인 사용자 이메일: {}", email);
//        }
//        // 현재 프로젝트 내에서 사용자의 ProjectMember 정보를 조회하여 역할을 모델에 추가
//        Optional<ProjectMember> currentMemberOpt = project.getProjectMembers().stream()
//                .filter(pm -> pm.getUser() != null && pm.getUser().getEmail().equalsIgnoreCase(email))
//                .findFirst();
//        if (currentMemberOpt.isPresent()) {
//            model.addAttribute("projectRole", currentMemberOpt.get().getProjectRole());
//        } else {
//            model.addAttribute("projectRole", null);
//        }
//
//        return "noticeBoardList"; // resources/templates/noticeBoardList.html
//    }
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    @GetMapping
    public String listNoticeBoards(@PathVariable("projectId") Long projectId,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model,
                                   Authentication authentication) {
        // 페이징 처리된 프로젝트별 게시글 목록 조회
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<NoticeBoardDTO> noticeBoardPage = noticeBoardService.getNoticeBoardsByProjectPaging(projectId, pageable);
        model.addAttribute("noticeBoards", noticeBoardPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", noticeBoardPage.getTotalPages());
        model.addAttribute("projectId", projectId);

        // 프로젝트 객체 조회 및 모델에 추가 (예: navbar에서 사용)
        Project project = projectService.getProjectById(projectId);
        model.addAttribute("project", project);

        // 현재 인증된 사용자 이메일 추출 (폼 로그인 / OAuth2 고려)
        String email;
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            email = (String) oauth2User.getAttribute("email");
            log.info("OAuth2 로그인 사용자 이메일: {}", email);
        } else {
            email = authentication.getName();
            log.info("일반 로그인 사용자 이메일: {}", email);
        }
        // 현재 프로젝트 내 사용자의 역할을 모델에 추가
        Optional<ProjectMember> currentMemberOpt = project.getProjectMembers().stream()
                .filter(pm -> pm.getUser() != null && pm.getUser().getEmail().equalsIgnoreCase(email))
                .findFirst();
        if (currentMemberOpt.isPresent()) {
            model.addAttribute("projectRole", currentMemberOpt.get().getProjectRole());
        } else {
            model.addAttribute("projectRole", null);
        }

        // 최신 글로벌 공지 전부 조회
        List<GlobalNoticeDTO> globalNotices = globalNoticeService.getAllGlobalNotices();
        model.addAttribute("globalNotices", globalNotices);

        return "noticeBoardList"; // 수정된 템플릿에서 글로벌 공지 표시
    }

    //게시글 상세보기
    @GetMapping("/{id}")
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public String detailNoticeBoard(@PathVariable("projectId") Long projectId,
                                    @PathVariable Long id, Model model) {
        NoticeBoardDTO noticeBoardDTO = noticeBoardService.getNoticeBoardByIdAndProject(id, projectId);
        model.addAttribute("noticeBoard", noticeBoardDTO);
        model.addAttribute("projectId", projectId);
        // projectId를 이용하여 프로젝트 객체를 조회 (서비스 또는 리포지토리 사용)
        Project project = projectService.getProjectById(projectId);//RYAN navbar에서 project.projectName 을 쓰기위해 모델 추가
        // 프로젝트 객체를 모델에 추가
        model.addAttribute("project", project);//RYAN navbar에서 project.projectName 을 쓰기위해 모델 추가
        return "noticeBoardDetail"; // resources/templates/noticeBoardDetail.html
    }

    //전체공지 상세보기
    @GetMapping("/global/{id}")
    public String detailGlobalNotice(@PathVariable("projectId") Long projectId,
                                     @PathVariable("id") Long id,
                                     Model model) {
        // 글로벌 공지 상세 정보 조회
        GlobalNoticeDTO globalNoticeDTO = globalNoticeService.getGlobalNoticeById(id);
        model.addAttribute("globalNotice", globalNoticeDTO);

        // 프로젝트 정보도 함께 전달 (예: navbar 등에서 사용)
        Project project = projectService.getProjectById(projectId);
        model.addAttribute("project", project);
        model.addAttribute("projectId", projectId);

        return "/admin/manage/globalNoticeDetail"; // 글로벌 공지 상세보기 템플릿 (resources/templates/globalNoticeDetail.html)
    }

    /**
     * ==========================================
     * 게시글 등록 페이지
     * URL: /board/noticeboard/create
     * - noticeBoardCreate.html : 새 게시글 등록 폼과 목록보기 버튼 포함
     */
    @GetMapping("/create")
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public String showCreateForm(@PathVariable("projectId") Long projectId, Model model) {
        model.addAttribute("noticeBoardDto", new NoticeBoardDTO());
        model.addAttribute("projectId", projectId);
        // projectId를 이용하여 프로젝트 객체를 조회 (서비스 또는 리포지토리 사용)
        Project project = projectService.getProjectById(projectId);//RYAN navbar에서 project.projectName 을 쓰기위해 모델 추가
        // 프로젝트 객체를 모델에 추가
        model.addAttribute("project", project);//RYAN navbar에서 project.projectName 을 쓰기위해 모델 추가
        return "noticeBoardCreate"; // resources/templates/noticeBoardCreate.html
    }

    //게시글 등록
    @PostMapping("/create")
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public String createNoticeBoard(@PathVariable("projectId") Long projectId,
                                    @Valid @ModelAttribute("noticeBoardDto") NoticeBoardDTO noticeBoardDTO,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "noticeBoardCreate";
        }
        log.info("LOG1 projectID : {}" ,projectId);//LOG
        NoticeBoard noticeBoard = new NoticeBoard();
        noticeBoard.setBoardNm("noticeboard");
        noticeBoard.setCreatedDate(LocalDateTime.now());
        // DTO의 값을 엔티티로 세팅 (예: 카테고리, 제목, 내용)
        noticeBoard.setTitle(noticeBoardDTO.getTitle());
        noticeBoard.setContents(noticeBoardDTO.getContents());
        log.info("LOG2 projectID : {}" ,projectId);//LOG
        noticeBoardService.saveNoticeBoard(noticeBoard, projectId);
        log.info("LOG3 projectID : {}" ,projectId);//LOG
        redirectAttributes.addFlashAttribute("message", "게시글이 등록되었습니다.");
        log.info("LOG4 projectID : {}" ,projectId);//LOG
//        redirectAttributes.addAttribute("projectId", projectId);
        log.info("LOG5 projectID : {}" ,projectId);//LOG
        return "redirect:/projects/" + projectId + "/board/noticeboard";
//        return "redirect:/projects/" + projectId + "/board/noticeboard";  // 등록 후 목록 페이지로 이동
    }

    //게시글 수정
    @PostMapping("/update/{id}")
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public String updateNoticeBoard(@PathVariable("projectId") Long projectId,
                                    @PathVariable Long id,
                                    @Valid @ModelAttribute("noticeBoardDto") NoticeBoardDTO noticeBoardDTO,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "noticeBoardDetail"; // 수정 실패 시 상세 페이지로 돌아감 (또는 별도의 에러 페이지 처리)
        }
        noticeBoardService.updateNoticeBoard(id, noticeBoardDTO, projectId);
        redirectAttributes.addFlashAttribute("message", "게시글이 수정되었습니다.");
        return "redirect:/projects/" + projectId + "/board/noticeboard/detail/" + id;
    }


    //게시글 삭제
    @PostMapping("/delete/{id}")
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public String deleteNoticeBoard(@PathVariable("projectId") Long projectId,
                                    @PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {
        noticeBoardService.deleteNoticeBoard(id, projectId);
        redirectAttributes.addFlashAttribute("message", "게시글이 삭제되었습니다.");
        return "redirect:/projects/" + projectId + "/board/noticeboard";
    }

}
