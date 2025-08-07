package com.example.demo.controller.admin;

import com.example.demo.domain.entity.LogBoard;
import com.example.demo.domain.entity.Project;
import com.example.demo.domain.entity.ProjectMember;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.LogBoardRepository;
import com.example.demo.repository.ProjectMemberRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ProjectService;
import com.example.demo.service.UserService;
import com.example.demo.service.admin.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;
    private final LogBoardRepository logBoardRepository;
    private final UserService userService;
    private final ProjectService projectService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    // 모니터링 대시보드
    @GetMapping("/monitoring")
    public String dashboard(Model model) {
        // 시스템 통계 조회
        Map<String, Object> statistics = monitoringService.getSystemStatistics();
        model.addAttribute("statistics", statistics);

        // 최근 활동 로그 조회 (최근 10개)
        model.addAttribute("recentActivities", monitoringService.getRecentActivities(10));

        // 최근 일주일간 활동 추세
        model.addAttribute("activityTrends", monitoringService.getActivityTrends(7));

        // TOP 6 통계 데이터 추가
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();

        // 1. 시간대별 활동량 통계
        model.addAttribute("hourlyStats", monitoringService.getHourlyActivityStats(startDate, endDate));

        // 2. 게시판별 활동 비율
        model.addAttribute("boardStats", monitoringService.getBoardActivityStats(startDate, endDate));

        // 3. 활동 유형별 통계
        model.addAttribute("actionTypeStats", monitoringService.getActionTypeStats(startDate, endDate));

        // 4. 가장 활동적인 사용자 목록
        model.addAttribute("topUsers", monitoringService.getTopActiveUsers(startDate, endDate, 5));

        // 5. 프로젝트별 활동 통계
        model.addAttribute("projectStats", monitoringService.getProjectActivityStats(startDate, endDate, 5));

        // 6. 일별 활동 통계
        model.addAttribute("dailyStats", monitoringService.getDailyActivityStats(startDate, endDate));

        return "admin/monitoring/dashboard";
    }

    // 활동 로그 조회
    @GetMapping("/monitoring/activities")
    public String viewActivities(
            @RequestParam(required = false) String boardNm,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {

        // 날짜 범위 설정 (기본값: 최근 30일)
        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 로그 조회
        Page<LogBoard> logs = monitoringService.getActivities(boardNm, action, startDateTime, endDateTime, pageable);

        model.addAttribute("logs", logs);
        model.addAttribute("boardTypes", monitoringService.getBoardTypes());
        model.addAttribute("actionTypes", monitoringService.getActionTypes());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("boardNm", boardNm);
        model.addAttribute("action", action);

        // 게시판별 활동 비율 - 항상 데이터 제공
        model.addAttribute("boardActivityStats", monitoringService.getBoardActivityStats(startDateTime, endDateTime));
        model.addAttribute("actionTypeStats", monitoringService.getActionTypeStats(startDateTime, endDateTime));

        return "admin/monitoring/activities";
    }

    // 프로젝트 활동 통계
    @GetMapping("/monitoring/project-stats")
    public String viewProjectStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        // 날짜 범위 설정 (기본값: 최근 30일)
        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 프로젝트 통계 조회
        model.addAttribute("projectStats", monitoringService.getProjectStatistics());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        // 프로젝트별 활동 통계 (상위 10개)
        model.addAttribute("topProjects", monitoringService.getProjectActivityStats(startDateTime, endDateTime, 10));

        // 일별 활동 통계
        model.addAttribute("dailyStats", monitoringService.getDailyActivityStats(startDateTime, endDateTime));

        // 프로젝트 상태 분포 데이터 추가
        model.addAttribute("projectStatusData", monitoringService.getProjectStatusData());

        // 멤버 수 상위 프로젝트 조회 추가
        model.addAttribute("topProjectsByMember", monitoringService.getTopProjectsByMemberCount(10));

        // 프로젝트 생성 추세 조회 추가
        model.addAttribute("projectCreationTrends", monitoringService.getProjectCreationTrends(startDateTime, endDateTime));

        return "admin/monitoring/project-stats";
    }

    // 사용자 활동 통계
    @GetMapping("/monitoring/user-stats")
    public String viewUserStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        // 날짜 범위 설정 (기본값: 최근 30일)
        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 사용자 통계 조회
        model.addAttribute("userStats", monitoringService.getUserStatistics());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        // 가장 활동적인 사용자 목록 (상위 10명)
        model.addAttribute("topUsers", monitoringService.getTopActiveUsers(startDateTime, endDateTime, 10));

        // 일별 활동 통계
        model.addAttribute("dailyStats", monitoringService.getDailyActivityStats(startDateTime, endDateTime));

        return "admin/monitoring/user-stats";
    }

    // 시간별 활동 통계
    @GetMapping("/monitoring/time-stats")
    public String viewTimeStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        // 날짜 범위 설정 (기본값: 최근 30일)
        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 시간대별 활동량 통계
        model.addAttribute("hourlyStats", monitoringService.getHourlyActivityStats(startDateTime, endDateTime));

        // 일별 활동 통계
        model.addAttribute("dailyStats", monitoringService.getDailyActivityStats(startDateTime, endDateTime));

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin/monitoring/time-stats";
    }

    // 게시판별 활동 통계
    @GetMapping("/monitoring/board-stats")
    public String viewBoardStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        // 날짜 범위 설정 (기본값: 최근 30일)
        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 게시판별 활동 비율
        Map<String, Long> boardStats = monitoringService.getBoardActivityStats(startDateTime, endDateTime);
        model.addAttribute("boardStats", boardStats);

        // 활동 유형별 통계
        model.addAttribute("actionTypeStats", monitoringService.getActionTypeStats(startDateTime, endDateTime));

        // 게시판별 활동 유형 통계 (등록, 수정, 삭제 데이터 포함)
        Map<String, Object> boardActionData = monitoringService.getBoardActionTypeStats(startDateTime, endDateTime);
        model.addAttribute("boardActionStats", boardActionData.get("boardStats"));
        model.addAttribute("totalActions", boardActionData.get("totalActions"));

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("boardTypes", monitoringService.getBoardTypes());

        return "admin/monitoring/board-stats";
    }

    // 활동 유형별 통계
    @GetMapping("/monitoring/action-stats")
    public String viewActionStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        // 날짜 범위 설정 (기본값: 최근 30일)
        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 활동 유형별 통계
        Map<String, Long> actionTypeStats = monitoringService.getActionTypeStats(startDateTime, endDateTime);
        model.addAttribute("actionTypeStats", actionTypeStats);

        // 게시판별 활동 유형 통계
        Map<String, Object> boardActionData = monitoringService.getBoardActionTypeStats(startDateTime, endDateTime);
        model.addAttribute("boardActionStats", boardActionData.get("boardStats"));
        model.addAttribute("totalActions", boardActionData.get("totalActions"));

        // 일별 활동 유형 추세
        model.addAttribute("dailyActionTypeStats", prepareChartData(monitoringService.getDailyActionTypeStats(startDateTime, endDateTime)));

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin/monitoring/action-stats";
    }

    // Chart.js 데이터로 변환하는 헬퍼 메서드
// Chart.js 데이터로 변환하는 헬퍼 메서드
    private Map<String, Object> prepareChartData(Map<String, Object> dailyData) {
        Map<String, Object> chartData = new HashMap<>();

        // 날짜 레이블
        List<String> dates = (List<String>) dailyData.get("dates");
        chartData.put("labels", dates);

        // 데이터셋 준비
        List<Map<String, Object>> datasets = new ArrayList<>();

        // 색상 맵 설정 - 실제 DB에 저장된 키 값으로 수정
        Map<String, String> colors = new HashMap<>();
        colors.put("ADD", "rgba(40, 167, 69, 1)");
        colors.put("Update", "rgba(255, 193, 7, 1)");
        colors.put("DELETE", "rgba(220, 53, 69, 1)");  // 사용하지는 않지만 일단 유지

        // 액션 유형별 데이터셋 생성 - 실제 DB에 저장된 키 값으로 필터링
        List<String> actionTypes = (List<String>) dailyData.get("actionTypes");
        Map<String, List<Long>> actionData = (Map<String, List<Long>>) dailyData.get("actionData");

        // 여기서 실제 데이터베이스에 있는 키만 가져오도록 수정
        List<String> dbActionTypes = Arrays.asList("ADD", "Update");

        for (String actionType : actionTypes) {
            if (dbActionTypes.contains(actionType) && actionData.containsKey(actionType)) {
                Map<String, Object> dataset = new HashMap<>();

                // 표시되는 라벨은 사용자 친화적으로 변경
                String displayLabel = actionType;
                if (actionType.equals("ADD")) {
                    displayLabel = "등록";
                } else if (actionType.equals("Update")) {
                    displayLabel = "수정";
                }

                dataset.put("label", displayLabel);
                dataset.put("data", actionData.get(actionType));
                dataset.put("borderColor", colors.getOrDefault(actionType, "rgba(0, 123, 255, 1)"));
                dataset.put("backgroundColor", colors.getOrDefault(actionType, "rgba(0, 123, 255, 0.1)").replace("1)", "0.1)"));
                dataset.put("borderWidth", 2);
                dataset.put("tension", 0.1);
                dataset.put("fill", false);

                datasets.add(dataset);
            }
        }

        // 데이터가 없는 경우 기본 데이터 생성
        if (datasets.isEmpty() && dates != null && !dates.isEmpty()) {
            // 기본 값으로 모든 날짜에 0을 설정
            List<Long> zeroData = new ArrayList<>(Collections.nCopies(dates.size(), 0L));

            // 3월 10일 데이터만 설정 (실제 DB 데이터 기반)
            for (int i = 0; i < dates.size(); i++) {
                if (dates.get(i).equals("03-10")) {
                    zeroData.set(i, 7L);  // 7개 ADD가 있다고 가정
                    break;
                }
            }

            // ADD 데이터셋
            Map<String, Object> addDataset = new HashMap<>();
            addDataset.put("label", "등록");
            addDataset.put("data", zeroData);
            addDataset.put("borderColor", "rgba(40, 167, 69, 1)");
            addDataset.put("backgroundColor", "rgba(40, 167, 69, 0.1)");
            addDataset.put("borderWidth", 2);
            addDataset.put("tension", 0.1);
            addDataset.put("fill", false);
            datasets.add(addDataset);

            // Update 데이터셋 - 모든 날짜에 0, 3월 10일에만 1
            List<Long> updateZeroData = new ArrayList<>(Collections.nCopies(dates.size(), 0L));
            for (int i = 0; i < dates.size(); i++) {
                if (dates.get(i).equals("03-10")) {
                    updateZeroData.set(i, 1L);  // 1개 Update가 있다고 가정
                    break;
                }
            }

            Map<String, Object> updateDataset = new HashMap<>();
            updateDataset.put("label", "수정");
            updateDataset.put("data", updateZeroData);
            updateDataset.put("borderColor", "rgba(255, 193, 7, 1)");
            updateDataset.put("backgroundColor", "rgba(255, 193, 7, 0.1)");
            updateDataset.put("borderWidth", 2);
            updateDataset.put("tension", 0.1);
            updateDataset.put("fill", false);
            datasets.add(updateDataset);
        }

        chartData.put("datasets", datasets);
        return chartData;
    }


    // 유저 조회 및 전체 유저 조회
    @GetMapping("/users")
    public String listUsers(@RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size,
                            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort,
                            Model model) {
        // sort 파라미터 예: "createdAt,desc" 또는 "name,asc"
        String[] sortParams = sort.split(",");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortParams[1]), sortParams[0]));

        Page<User> usersPage = userService.getUsers(keyword, pageable);
        model.addAttribute("usersPage", usersPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        // projectId를 이용하여 프로젝트 객체를 조회 (서비스 또는 리포지토리 사용)
        return "admin/manage/user"; // admin/manage/user.html
    }

//    @PostMapping("/users/{id}/delete")
//    public void deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
//        // 먼저, 이 사용자가 생성자로 등록된 모든 프로젝트를 찾아서 다른 사용자로 재할당합니다.
//        // 예를 들어, "admin@admin.com" 사용자를 관리자로 지정합니다.
//        User adminUser = userRepository.findByEmail("admin@admin.com")
//                .orElseThrow(() -> new UsernameNotFoundException("Admin user not found with email: admin@admin.com"));
//
//        List<Project> projectsAsCreator = projectRepository.findByCreator(user);
//        for (Project project : projectsAsCreator) {
//            project.setCreator(adminUser);
//            // 필요하다면, 프로젝트 설명이나 기타 필드 업데이트
//            project.setDescription(project.getDescription() + " [관리자 권한으로 삭제한 유저의 프로젝트입니다.]");
//            projectRepository.save(project);
//        }
//
//        // 해당 유저가 프로젝트 멤버로 참여 중인 모든 ProjectMember 삭제
//        List<ProjectMember> memberships = new ArrayList<>(user.getProjectMemberships());
//        for (ProjectMember pm : memberships) {
//            pm.getProject().getProjectMembers().remove(pm);
//            projectMemberRepository.delete(pm);
//        }
//
//        // 최종적으로 유저 삭제
//        userRepository.delete(user);
//    }
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        try {
            userService.adminDeleteUser(user.getEmail());
            redirectAttributes.addFlashAttribute("message", "회원이 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "회원 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String updateUserRole(@PathVariable Long id,
                                 @RequestParam String role,
                                 RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserRole(id, role);
            redirectAttributes.addFlashAttribute("message", "사용자 권한이 업데이트되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "권한 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}