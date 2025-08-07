package com.example.demo.controller;

import com.example.demo.domain.entity.LogBoard;
import com.example.demo.domain.entity.Project;
import com.example.demo.service.LogBoardService;
import com.example.demo.service.NoticeBoardService;
import com.example.demo.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
//@RequestMapping("/logs")
@RequestMapping("/projects/{projectId}/board")
@RequiredArgsConstructor
public class LogBoardController {

    private static final Logger log = LoggerFactory.getLogger(LogBoardController.class);
    private final LogBoardService logBoardService;
    private final NoticeBoardService noticeBoardService;
    private final ProjectService projectService;
    //다른 게시판도 logboard에 필요하면  다른 게시판에 대한 서비스계층도 추가하면됨

    // GET /logs 경로 접속시 logBoard.html 표시
    @GetMapping("/logboard")
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public String logBoardPage(
            @PathVariable("projectId") Long projectId,
            Model model,
            @PageableDefault(size = 20, sort = "logNo", direction = Sort.Direction.DESC) Pageable pageable) {
        // projectId에 해당하는 로그만 필터링해서 조회하도록 service 메소드 호출
        Page<LogBoard> logPage = logBoardService.getLogsByProject(projectId, pageable);
        model.addAttribute("logs", logPage.getContent());
        model.addAttribute("page", logPage);
        model.addAttribute("projectId", projectId);
        // projectId를 이용하여 프로젝트 객체를 조회 (서비스 또는 리포지토리 사용)
        Project project = projectService.getProjectById(projectId);//RYAN navbar에서 project.projectName 을 쓰기위해 모델 추가
        // 프로젝트 객체를 모델에 추가
        model.addAttribute("project", project);//RYAN navbar에서 project.projectName 을 쓰기위해 모델 추가
        return "logBoard";
    }

    @GetMapping("/logboard/")
    @ResponseBody
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public ResponseEntity<Page<LogBoard>> getLogsApi(@PathVariable("projectId") Long projectId,
                                                     @PageableDefault(size = 100) Pageable pageable) {
        return ResponseEntity.ok(logBoardService.getLogsByProject(projectId, pageable));
    }
    //다른 게시판 생성되면
    //private final LogBoardSerivce logBoardService; 주입 후
    //logBoardService.saveLog("A 게시판", "ADD", "새 글 등록: " + post.getTitle(), userName); 메서드 호출

    // LogBoard 목록 클릭시 해당 게시판 상세보기로 이동하는 매핑
    // GET /board/{boardNm}/{id} : 게시글 상세보기
    // (예를 들어, noticeboard 게시글 상세보기)
    @GetMapping("/{boardNm}/{id}")
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public String viewPost(
            @RequestParam("projectId") Long projectId, // 혹은 경로변수에 포함하도록 수정 가능
            @PathVariable String boardNm,
            @PathVariable Long id,
            Model model) {
        if ("noticeboard".equalsIgnoreCase(boardNm)) {
            // projectId와 id를 이용하여 해당 noticeboard 게시글 조회
            var notice = noticeBoardService.getNoticeBoardByIdAndProject(id, projectId);
            model.addAttribute("notice", notice);
            model.addAttribute("projectId", projectId);
            // projectId를 이용하여 프로젝트 객체를 조회 (서비스 또는 리포지토리 사용)
            Project project = projectService.getProjectById(projectId);//RYAN navbar에서 project.projectName 을 쓰기위해 모델 추가
            // 프로젝트 객체를 모델에 추가
            model.addAttribute("project", project);//RYAN navbar에서 project.projectName 을 쓰기위해 모델 추가
            return "noticeBoardDetail"; // noticeboard 게시글 상세 페이지 템플릿 이름
        } else if ("calendar".equalsIgnoreCase(boardNm)) {
            // calendar일 경우 /projects/{projectId}로 이동
            return "redirect:/projects/" + projectId;
        } else if ("Issue".equalsIgnoreCase(boardNm)) {
            // Issue일 경우 /projects/{projectId}/issues/{id}로 이동
            return "redirect:/projects/" + projectId + "/issues/" + id;
        }
        // 추가 게시판이 있으면 else if로 처리
        return "redirect:/error";
    }
}
