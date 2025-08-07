package com.example.demo.controller;

import com.example.demo.domain.dto.CalendarEventDTO;
import com.example.demo.domain.dto.IssueDTO;
import com.example.demo.domain.dto.NoticeBoardDTO;
import com.example.demo.domain.dto.ProjectRequest;
import com.example.demo.domain.entity.*;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectViewController {
    private final ProjectService projectService;
    private final UserRepository userRepository;
    private final NoticeBoardService noticeBoardService;
    private final IssueService issueService;
    private final LogBoardService logBoardService;

    private final CalendarEventService calendarEventService;

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("projectRequest", new ProjectRequest());
        return "projects/createForm";
    }

    @GetMapping
    public String listProjects(
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        // 현재 사용자 이메일 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = null;

        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            email = (String) oauth2User.getAttribute("email");
            log.info("OAuth2 로그인 사용자 이메일: {}", email);
        } else {
            email = authentication.getName();
            log.info("일반 로그인 사용자 이메일: {}", email);
        }
        final String userEmail = email;

        List<Project> projects = new ArrayList<>();

        // role 파라미터에 따른 프로젝트 조회
        if ("leader".equalsIgnoreCase(role)) {
            // [리더] 버튼 클릭 시 리더로 속한 프로젝트만 조회
            projects = projectService.getProjectsByLeader(userEmail);
        } else if ("member".equalsIgnoreCase(role)) {
            // [멤버] 버튼 클릭 시 멤버로 속한 프로젝트만 조회
            projects = projectService.getProjectsByMember(userEmail);
        } else if ("admin".equalsIgnoreCase(role)) {
            // [Admin] 버튼 클릭 시 Admin으로 속한 프로젝트만 조회
            projects = projectService.getProjectsByAdmin(userEmail);
        } else {
            // 기본: 전체 프로젝트 (리더, 멤버 모두 해당)
            List<Project> leaderProjects = projectService.getProjectsByLeader(userEmail);
            List<Project> memberProjects = projectService.getProjectsByMember(userEmail);
//            List<Project> adminProjects = projectService.getProjectsByAdmin(userEmail);
            projects = Stream.concat(leaderProjects.stream(), memberProjects.stream())
                    .distinct()
                    .collect(Collectors.toList());
        }

        // 검색기간이 지정된 경우, 해당 기간 내에 포함되는 프로젝트만 필터링
        if (startDate != null && endDate != null) {
            projects = projects.stream()
                    .filter(project -> {
                        // LocalDateTime을 LocalDate로 변환
                        LocalDate projectStartDate = project.getStartDate().toLocalDate();
                        LocalDate projectEndDate = project.getEndDate().toLocalDate();
                        return projectStartDate.compareTo(startDate) >= 0 &&
                                projectEndDate.compareTo(endDate) <= 0;
                    })
                    .collect(Collectors.toList());
        }
        model.addAttribute("projects", projects);
        return "projects/list";  // list.html에 해당하는 뷰 이름
    }


    @PostMapping
    public String createProject(
            @Valid @ModelAttribute ProjectRequest projectRequest,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "projects/createForm";
        }

        // SecurityContext에서 현재 인증된 사용자 정보를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 디버깅을 위한 로그 추가
        System.out.println("=== 인증 정보 디버깅 ===");
        System.out.println("Authentication Class: " + authentication.getClass().getName());
        System.out.println("Principal Class: " + authentication.getPrincipal().getClass().getName());
        System.out.println("Principal: " + authentication.getPrincipal());
        System.out.println("Credentials: " + authentication.getCredentials());
        System.out.println("Name: " + authentication.getName());
        System.out.println("Details: " + authentication.getDetails());

        // Principal이 OAuth2User인 경우를 처리
        String email;
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            email = oauth2User.getAttribute("email");
            System.out.println("OAuth2 Email: " + email);
        } else {
            email = authentication.getName();
            System.out.println("Regular Email: " + email);
        }

        // 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. Email: " + email));

        projectService.createProject(projectRequest, user);
        redirectAttributes.addFlashAttribute("message", "프로젝트가 생성되었습니다.");
        return "redirect:/projects";
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public String viewProject(@PathVariable Long projectId, Model model, Authentication authentication) {
        String email;
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            email = (String) oauth2User.getAttribute("email");
            log.info("OAuth2 로그인 사용자 이메일: {}", email);
        } else {
            email = authentication.getName();
            log.info("일반 로그인 사용자 이메일: {}", email);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        Project project = projectService.getProject(projectId, user);
        model.addAttribute("project", project);

        // projectId를 명시적으로 모델에 추가 (캘린더 관련)
        model.addAttribute("projectId", projectId);

        // 최근 공지사항 3개 추가
        List<NoticeBoardDTO> latestNotices = noticeBoardService.getAllNoticeBoardsByProject(projectId)
                .stream()
                .sorted(Comparator.comparing(NoticeBoardDTO::getCreatedDate).reversed())
                .limit(3)
                .collect(Collectors.toList());
        model.addAttribute("latestNotices", latestNotices);

        // 최근 이슈 3개 추가
        List<IssueDTO> latestIssues = issueService.getAllIssuesByProject(projectId)
                .stream()
                .sorted(Comparator.comparing(IssueDTO::getCreatedDate).reversed())
                .limit(3)
                .collect(Collectors.toList());
        model.addAttribute("latestIssues", latestIssues);

        // 최근 로그 추가
        List<LogBoard> latestLogs = logBoardService.getLatestLogs(projectId);
        model.addAttribute("latestLogs", latestLogs);

        // 사용자 이름 추가
        model.addAttribute("username", user.getName());

        return "projects/projectHome";  // 경로 수정 (맨 앞 슬래시 제거)
    }
    // 관리자용 전체 프로젝트 목록 조회
    @GetMapping("/admin/projectlist")
    public String listAllProjects(Model model) {
        // 현재 사용자의 권한 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            // 관리자가 아닌 경우 접근 거부 (403 에러 페이지)
            return "error/403";
        }
        List<Project> projects = projectService.getAllProjects();
        model.addAttribute("projects", projects);
        return "adminProjectList"; // 관리자용 전체 프로젝트 목록 뷰[미구현]
    }

    //프로젝트 관리 페이지로 이동 매핑 + 프로젝트 수정(모달)
    @GetMapping("/{projectId}/management")
    public String managementPage(@PathVariable("projectId") Long projectId,
                                 @RequestParam(value = "query", required = false) String query,
                                 Model model,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        // 현재 로그인한 사용자의 이메일 추출
        String email;
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            email = (String) oauth2User.getAttribute("email");
            log.info("OAuth2 로그인 사용자 이메일: {}", email);
        } else {
            email = authentication.getName();
            log.info("일반 로그인 사용자 이메일: {}", email);
        }
        model.addAttribute("email", email);

        // 검색어(query)가 존재하면 사용자 검색 수행
        if (query != null && !query.trim().isEmpty()) {
            List<User> users = userRepository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(query, query);
            model.addAttribute("users", users);
            model.addAttribute("query", query);  // 검색어도 모델에 담아 뷰에 전달 (예: 검색 결과 화면에 표시)
            log.info("LOG Manage USERS : {} ", users);
        }
        log.info("LOG Manage PROJECT ID : {} ", projectId);
        log.info("LOG Manage QUERY : {} ", query);

        try {
            // 도메인 User 객체 조회
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

            // 보안 검증까지 포함한 프로젝트 조회
            Project project = projectService.getProject(projectId, user);
            model.addAttribute("project", project);

            // 프로젝트에 속한 사용자 목록 추가 (프로젝트 멤버 전체를 모델에 담음)
            List<ProjectMember> projectMembers = project.getProjectMembers();
            model.addAttribute("projectMembers", projectMembers);

            // 현재 사용자의 프로젝트 역할(ProjectRole)을 찾아 모델에 추가
            Optional<ProjectMember> currentMemberOpt = projectMembers.stream()
                    .filter(pm -> pm.getUser().getId().equals(user.getId()))
                    .findFirst();
            if (currentMemberOpt.isPresent()) {
                ProjectRole currentRole = currentMemberOpt.get().getProjectRole();
                model.addAttribute("projectRole", currentRole);
            } else {
                // 해당 프로젝트에 속한 회원 정보가 없다면 기본값 또는 예외 처리
                model.addAttribute("projectRole", null);
            }


            // ProjectRequest로 변환하여 폼에 기본값 세팅
            ProjectRequest projectRequest = new ProjectRequest();
            projectRequest.setProjectName(project.getProjectName());//프로젝트명 수정
            projectRequest.setDescription(project.getDescription());//프로젝트 설명 수정
            projectRequest.setStartDate(project.getStartDate().toLocalDate().toString());//프로젝트 시작일 수정 
            projectRequest.setEndDate(project.getEndDate().toLocalDate().toString());//프로젝트 종료일 수정
            projectRequest.setStatus(project.getStatus());//프로젝트 상태
            model.addAttribute("projectRequest", projectRequest);
//            return "projects/editForm";  // 수정 폼 템플릿 (projects/editForm.html)
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects" + projectId;
        }
        return "projects/management";  // templates/management.html
    }


    @PostMapping("/{projectId}/management/updateMemberRole/{userId}")
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public String updateMemberRole(@PathVariable Long projectId,
                                   @PathVariable Long userId,
                                   @RequestParam String projectRole,
                                   RedirectAttributes redirectAttributes) {
        try {
            // userId와 projectId를 기반으로 해당 멤버의 프로젝트 역할을 업데이트하는 서비스 로직 호출
            projectService.updateProjectMemberRole(projectId, userId, ProjectRole.valueOf(projectRole));
            redirectAttributes.addFlashAttribute("message", "멤버 권한이 업데이트되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "멤버 권한 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
        return "redirect:/projects/" + projectId + "/management";
    }

    @GetMapping("/{projectId}/management/search")
    public String searchUsers(@RequestParam("query") String query,
                              @PathVariable("projectId") Long projectId,
                              Authentication authentication,  // Principal 대신 Authentication 사용
                              Model model) {
        // 현재 사용자의 이메일 가져오기
        String currentUserEmail;
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            currentUserEmail = oauth2User.getAttribute("email");
        } else {
            currentUserEmail = authentication.getName();
        }

        // 검색어에 해당하는 사용자 목록 검색하고 현재 사용자 제외
        List<User> users = userRepository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(query, query)
                .stream()
                .filter(user -> !user.getEmail().equals(currentUserEmail))  // 현재 사용자 제외
                .collect(Collectors.toList());

        model.addAttribute("users", users);
        model.addAttribute("projectId", projectId);
        model.addAttribute("query", query);

        log.info("LOG Search USERS : {}", users);
        log.info("LOG Search PROJECT ID : {}", projectId);
        log.info("LOG Search QUERY : {}", query);

        return "projects/management :: searchResults";
    }

    // 프로젝트 수정 처리 (POST /projects/{projectId}/edit)
    @PostMapping("/{projectId}/management/edit")
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    @Transactional
    public String updateProject(@PathVariable("projectId") Long projectId,
                                @Valid @ModelAttribute("projectRequest") ProjectRequest projectRequest,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Authentication authentication) {
        if (bindingResult.hasErrors()) {
//            return "projects/editForm";
            return "projects/management";
        }

        // Authentication 객체에서 이메일 추출
        String email;
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            email = (String) oauth2User.getAttribute("email");
        } else {
            email = authentication.getName();
        }

        // 도메인 User 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        try {
            // 프로젝트 수정 및 버전 체크
            Project updatedProject = projectService.updateProject(projectId, projectRequest, user);

            // 성공적으로 수정되었으면 리다이렉트
            redirectAttributes.addFlashAttribute("message", "프로젝트가 성공적으로 수정되었습니다.");
            return "redirect:/projects/" + updatedProject.getId();
        } catch (OptimisticLockingFailureException ex) {
            // 버전 충돌 예외 처리
            redirectAttributes.addFlashAttribute("error", "프로젝트 수정 중 버전 충돌이 발생했습니다. 다시 시도하십시오.");
            return "redirect:/projects/" + projectId + "/edit";
        } catch (Exception e) {
            // 다른 예외 처리
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId + "/edit";
        }
    }

    // 프로젝트 삭제 기능
    @PostMapping("/delete/{projectId}")
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    @Transactional
    public String deleteProject(@PathVariable Long projectId,
                                RedirectAttributes redirectAttributes,
                                Authentication authentication) {
        // Authentication 객체에서 이메일 추출
        String email;
        if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            email = ((org.springframework.security.core.userdetails.User) authentication.getPrincipal()).getUsername();
        } else if (authentication.getPrincipal() instanceof OAuth2User) {
            email = ((OAuth2User) authentication.getPrincipal()).getAttribute("email");
        } else {
            email = authentication.getName();
        }

        // 도메인 User 객체 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        // 현재 인증된 사용자 정보 로그 출력
        System.out.println("현재 인증된 User id: " + user.getId() + ", email: " + user.getEmail());

        // 삭제할 프로젝트 조회 (프로젝트 ID만 사용)
        Project project = projectService.getProjectById(projectId);
        if (project == null) {
            redirectAttributes.addFlashAttribute("error", "프로젝트를 찾을 수 없습니다.");
            return "redirect:/projects";
        }

        // 현재 사용자가 관리자(Admin)인지 확인
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        // 프로젝트 멤버 중 현재 사용자의 정보를 찾아 해당 사용자의 역할을 확인 (이메일 기준, 대소문자 무시)
        final String currentUserEmail = email;
        Optional<ProjectMember> projectMemberOpt = project.getProjectMembers().stream()
                .filter(pm -> pm.getUser() != null && pm.getUser().getEmail().equalsIgnoreCase(currentUserEmail))
                .findFirst();
        boolean hasProjectRole = projectMemberOpt.isPresent() &&
                (projectMemberOpt.get().getProjectRole() == ProjectRole.Leader ||
                        projectMemberOpt.get().getProjectRole() == ProjectRole.Admin);

        // 권한 체크: 관리자이거나 프로젝트 멤버 중 Leader/Admin 역할을 가진 경우에만 삭제 허용
        if (!isAdmin && !hasProjectRole) {
            redirectAttributes.addFlashAttribute("error", "프로젝트 삭제 권한이 없습니다.");
            return "redirect:/projects";
        }

        // 권한이 확인되면 삭제 진행
        try {
            projectService.deleteProject(projectId, user);
            redirectAttributes.addFlashAttribute("message", "프로젝트가 삭제되었습니다.");
        } catch (OptimisticLockingFailureException ex) {
            redirectAttributes.addFlashAttribute("error", "프로젝트 삭제 중 버전 충돌이 발생했습니다. 다시 시도하십시오.");
        }
        return "redirect:/projects";
    }

    //프로젝트 나가기
    @PostMapping("/{projectId}/management/exit")
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public String exitProject(@PathVariable Long projectId,
                              RedirectAttributes redirectAttributes,
                              Authentication authentication) {
        // Authentication 객체에서 이메일 추출
        String email;
        if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            email = ((org.springframework.security.core.userdetails.User) authentication.getPrincipal()).getUsername();
        } else if (authentication.getPrincipal() instanceof OAuth2User) {
            email = ((OAuth2User) authentication.getPrincipal()).getAttribute("email");
        } else {
            email = authentication.getName();
        }

        // 도메인 User 객체 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        try {
            projectService.leaveProject(projectId, user);
            redirectAttributes.addFlashAttribute("message", "프로젝트에서 탈퇴하였습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/{projectId}/events")
    @ResponseBody
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public List<CalendarEventDTO> getEvents(
            @PathVariable("projectId") Long projectId,
            @RequestParam String start,  // DateTimeFormat 어노테이션 제거
            @RequestParam String end     // DateTimeFormat 어노테이션 제거
    ) {
        // String을 LocalDateTime으로 변환
        LocalDateTime startDate = LocalDateTime.parse(start, DateTimeFormatter.ISO_DATE_TIME);
        LocalDateTime endDate = LocalDateTime.parse(end, DateTimeFormatter.ISO_DATE_TIME);

        return calendarEventService.getEventsByDateRange(projectId, startDate, endDate);
    }

    @PostMapping("/{projectId}/events")
    @ResponseBody
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public CalendarEventDTO createEvent(@PathVariable("projectId") Long projectId,
                                        @Valid @RequestBody CalendarEventDTO eventDTO) {
        return calendarEventService.createEvent(projectId, eventDTO);
    }

    @PutMapping("/{projectId}/events/{eventId}")
    @ResponseBody
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public CalendarEventDTO updateEvent(@PathVariable("projectId") Long projectId,
                                        @PathVariable("eventId") Long eventId,
                                        @Valid @RequestBody CalendarEventDTO eventDTO) {
        return calendarEventService.updateEvent(projectId, eventId, eventDTO);
    }

    @DeleteMapping("/{projectId}/events/{eventId}")
    @ResponseBody
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public void deleteEvent(@PathVariable("projectId") Long projectId,
                            @PathVariable("eventId") Long eventId) {
        calendarEventService.deleteEvent(projectId, eventId);
    }
}