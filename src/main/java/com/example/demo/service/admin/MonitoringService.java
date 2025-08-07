package com.example.demo.service.admin;

import com.example.demo.domain.entity.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MonitoringService {

    private final LogBoardRepository logBoardRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final NoticeBoardRepository noticeBoardRepository;
    private final IssueRepository issueRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final ProjectMemberRepository projectMemberRepository;

    // 최근 활동 로그 조회
    public List<LogBoard> getRecentActivities(int limit) {
        return logBoardRepository.findTop10ByOrderByActionDateDesc();
    }

    // 활동 로그 조회 - 필터링 기능 추가
    public Page<LogBoard> getActivities(String boardNm, String action, LocalDateTime startDateTime, LocalDateTime endDateTime, Pageable pageable) {
        // 날짜 범위가 지정된 경우
        if (startDateTime != null && endDateTime != null) {
            // 게시판과 액션이 모두 지정된 경우
            if (boardNm != null && !boardNm.isEmpty() && action != null && !action.isEmpty()) {
                return logBoardRepository.findByBoardNmAndActionAndActionDateBetween(
                        boardNm, action, startDateTime, endDateTime, pageable);
            }
            // 게시판만 지정된 경우
            else if (boardNm != null && !boardNm.isEmpty()) {
                return logBoardRepository.findByBoardNmAndActionDateBetween(
                        boardNm, startDateTime, endDateTime, pageable);
            }
            // 액션만 지정된 경우
            else if (action != null && !action.isEmpty()) {
                return logBoardRepository.findByActionAndActionDateBetween(
                        action, startDateTime, endDateTime, pageable);
            }
            // 날짜 범위만 지정된 경우
            else {
                return logBoardRepository.findByActionDateBetween(startDateTime, endDateTime, pageable);
            }
        }
        // 날짜 범위가 지정되지 않은 경우
        else {
            // 게시판과 액션이 모두 지정된 경우
            if (boardNm != null && !boardNm.isEmpty() && action != null && !action.isEmpty()) {
                return logBoardRepository.findByBoardNmAndAction(boardNm, action, pageable);
            }
            // 게시판만 지정된 경우
            else if (boardNm != null && !boardNm.isEmpty()) {
                return logBoardRepository.findByBoardNm(boardNm, pageable);
            }
            // 액션만 지정된 경우
            else if (action != null && !action.isEmpty()) {
                return logBoardRepository.findByAction(action, pageable);
            }
            // 아무 필터도 지정되지 않은 경우
            else {
                return logBoardRepository.findAll(pageable);
            }
        }
    }

    // 게시판 유형 목록 조회
    public List<String> getBoardTypes() {
        // 실제 존재하는 게시판 유형 목록을 반환
        return Arrays.asList("noticeboard", "issues", "calendar", "invitation", "comment");
    }

    // 액션 유형 목록 조회
    public List<String> getActionTypes() {
        // 가능한 액션 유형 목록을 반환
        return Arrays.asList("ADD", "Update");
    }

    // 시스템 통계 정보 조회
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 사용자 통계
        long totalUsers = userRepository.count();
        long adminUsers = userRepository.countByRole(Role.ADMIN);
        long regularUsers = totalUsers - adminUsers;

        // 프로젝트 통계
        long totalProjects = projectRepository.count();

        // 게시물 통계
        long totalNotices = noticeBoardRepository.count();
        long totalIssues = issueRepository.count();
        long totalEvents = calendarEventRepository.count();

        // 활동 통계
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long recentActivities = logBoardRepository.countByActionDateAfter(weekAgo);

        // 통계 데이터 추가
        statistics.put("totalUsers", totalUsers);
        statistics.put("regularUsers", regularUsers);
        statistics.put("adminUsers", adminUsers);
        statistics.put("totalProjects", totalProjects);
        statistics.put("totalNotices", totalNotices);
        statistics.put("totalIssues", totalIssues);
        statistics.put("totalEvents", totalEvents);
        statistics.put("recentActivities", recentActivities);

        return statistics;
    }

    public Map<String, Object> getProjectStatistics() {
        Map<String, Object> projectStats = new HashMap<>();

        // 전체 프로젝트 수 추가
        long totalProjects = projectRepository.count();
        projectStats.put("totalProjects", totalProjects);

        LocalDateTime startDateTime = LocalDateTime.now().minusDays(30);
        LocalDateTime endDateTime = LocalDateTime.now();

        // 프로젝트별 활동 통계 (상위 5개)
        Pageable pageable = PageRequest.of(0, 5);
        List<Object[]> projectActivities = logBoardRepository.countActivitiesByProject(startDateTime, endDateTime, pageable);

        List<Map<String, Object>> topProjects = new ArrayList<>();
        for (Object[] row : projectActivities) {
            Map<String, Object> project = new HashMap<>();
            project.put("projectId", row[0]);
            project.put("projectName", row[1]);
            project.put("activityCount", row[2]);
            topProjects.add(project);
        }

        // 기간 내 생성된 프로젝트 수 계산
        List<Object[]> creationData = projectRepository.countProjectsCreatedAfter(startDateTime);
        long projectsCreatedInPeriod = 0;
        if (creationData != null) {
            for (Object[] row : creationData) {
                Long count = (Long) row[1];
                projectsCreatedInPeriod += count;
            }
        }

        projectStats.put("topProjects", topProjects);
        projectStats.put("activeProjectCount", topProjects.size());
        projectStats.put("projectsCreatedInPeriod", projectsCreatedInPeriod);

        return projectStats;
    }

    // 사용자 활동 통계 정보 조회
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> userStats = new HashMap<>();

        LocalDateTime startDateTime = LocalDateTime.now().minusDays(30);
        LocalDateTime endDateTime = LocalDateTime.now();

        // 가장 활동적인 사용자 (상위 10명)
        Pageable pageable = PageRequest.of(0, 10);
        List<Object[]> activeUsers = logBoardRepository.findTopActiveUsers(startDateTime, endDateTime, pageable);

        List<Map<String, Object>> topUsers = new ArrayList<>();
        for (Object[] row : activeUsers) {
            Map<String, Object> user = new HashMap<>();
            user.put("userName", row[0]);
            user.put("activityCount", row[1]);
            topUsers.add(user);
        }

        userStats.put("topUsers", topUsers);

        return userStats;
    }

    // 시간대별 활동량 통계 - 개선된 버전
    public Map<String, Object> getHourlyActivityStats(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Map<String, Object> result = new HashMap<>();

        // 24시간 레이블과 카운트 배열 초기화
        List<String> labels = new ArrayList<>();
        List<Long> counts = new ArrayList<>();

        // 24시간에 대한 기본 구조 생성 (0시부터 23시까지)
        for (int i = 0; i < 24; i++) {
            labels.add(i + "시");
            counts.add(0L); // 기본값은 0
        }

        // DB에서 데이터 조회
        List<Object[]> hourlyStats = logBoardRepository.countHourlyActivities(startDateTime, endDateTime);

        // DB에서 조회한 데이터로 counts 업데이트
        for (Object[] row : hourlyStats) {
            Integer hour = ((Number) row[0]).intValue();
            Long count = (Long) row[1];

            // 유효한 시간 범위인지 확인 (0-23)
            if (hour >= 0 && hour < 24) {
                counts.set(hour, count); // 해당 시간의 카운트 업데이트
            }
        }

        result.put("labels", labels);
        result.put("counts", counts);

        return result;
    }

    // 2. 게시판별 활동 비율
    public Map<String, Long> getBoardActivityStats(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Map<String, Long> boardStats = new HashMap<>();

        List<Object[]> boardCounts = logBoardRepository.countByBoardNmGroupBy(startDateTime, endDateTime);
        for (Object[] row : boardCounts) {
            String boardType = (String) row[0];
            Long count = (Long) row[1];
            boardStats.put(boardType, count);
        }

        return boardStats;
    }

    // 3. 활동 유형별 통계
    public Map<String, Long> getActionTypeStats(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Map<String, Long> actionStats = new HashMap<>();

        List<Object[]> actionCounts = logBoardRepository.countByActionGroupBy(startDateTime, endDateTime);
        for (Object[] row : actionCounts) {
            String actionType = (String) row[0];
            Long count = (Long) row[1];
            actionStats.put(actionType, count);
        }

        return actionStats;
    }

    // 4. 가장 활동적인 사용자 목록
    public List<Map<String, Object>> getTopActiveUsers(LocalDateTime startDateTime, LocalDateTime endDateTime, int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> topUsers = logBoardRepository.findTopActiveUsers(startDateTime, endDateTime, pageable);

        for (Object[] row : topUsers) {
            Map<String, Object> userStat = new HashMap<>();
            String userName = (String) row[0];
            Long count = (Long) row[1];

            userStat.put("userName", userName);
            userStat.put("activityCount", count);
            result.add(userStat);
        }

        return result;
    }

    // 5. 프로젝트별 활동 통계
    public List<Map<String, Object>> getProjectActivityStats(LocalDateTime startDateTime, LocalDateTime endDateTime, int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> projectStats = logBoardRepository.countActivitiesByProject(startDateTime, endDateTime, pageable);

        for (Object[] row : projectStats) {
            Map<String, Object> projectStat = new HashMap<>();
            Long projectId = (Long) row[0];
            String projectName = (String) row[1];
            Long count = (Long) row[2];

            projectStat.put("projectId", projectId);
            projectStat.put("projectName", projectName);
            projectStat.put("activityCount", count);
            result.add(projectStat);
        }

        return result;
    }

    // 일별 활동 통계 - 개선된 버전
    public Map<String, Object> getDailyActivityStats(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Map<String, Object> result = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        // 날짜 범위에 대한 모든 날짜 레이블과 카운트 초기화
        List<String> labels = new ArrayList<>();
        List<Long> counts = new ArrayList<>();

        // startDateTime부터 endDateTime까지의 모든 날짜에 대한 기본 구조 생성
        LocalDateTime currentDate = startDateTime.toLocalDate().atStartOfDay();
        while (!currentDate.isAfter(endDateTime)) {
            labels.add(currentDate.format(formatter));
            counts.add(0L); // 기본값은 0
            currentDate = currentDate.plusDays(1);
        }

        // 날짜가 없으면 기본값으로 오늘 하나만 표시
        if (labels.isEmpty()) {
            labels.add(LocalDateTime.now().format(formatter));
            counts.add(0L);
        }

        // DB에서 데이터 조회
        List<Object[]> dailyStats = logBoardRepository.countDailyActivities(startDateTime, endDateTime);

        // DB 데이터를 날짜별로 매핑하기 위한 맵 생성
        Map<String, Long> dailyDataMap = new HashMap<>();
        for (Object[] row : dailyStats) {
            try {
                java.sql.Date date = (java.sql.Date) row[0];
                Long count = (Long) row[1];
                String dateStr = date.toLocalDate().format(formatter);
                dailyDataMap.put(dateStr, count);
            } catch (Exception e) {
                log.error("일별 활동 통계 데이터 처리 중 오류 발생: {}", e.getMessage());
            }
        }

        // 조회된 데이터로 counts 업데이트
        for (int i = 0; i < labels.size(); i++) {
            String dateLabel = labels.get(i);
            if (dailyDataMap.containsKey(dateLabel)) {
                counts.set(i, dailyDataMap.get(dateLabel));
            }
        }

        result.put("labels", labels);
        result.put("counts", counts);

        return result;
    }

    // 활동 추세 데이터 조회 (최근 N일간)
    public Map<String, Object> getActivityTrends(int days) {
        Map<String, Object> trends = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        // 시작 날짜 계산
        LocalDateTime startDate = LocalDateTime.now().minusDays(days - 1).toLocalDate().atStartOfDay();

        // 일별 활동 수 조회
        List<Object[]> dailyActivities = logBoardRepository.countDailyActivitiesAfter(startDate);

        // 데이터 포맷팅
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        // 현재로부터 N일 전까지의 날짜별 데이터 생성
        Map<String, Long> activityMap = new HashMap<>();
        for (Object[] row : dailyActivities) {
            java.sql.Date date = (java.sql.Date) row[0];
            Long count = (Long) row[1];
            activityMap.put(date.toLocalDate().format(formatter), count);
        }

        // 날짜 레이블 생성 및 데이터 매핑
        for (int i = days - 1; i >= 0; i--) {
            String label = LocalDateTime.now().minusDays(i).toLocalDate().format(formatter);
            labels.add(label);
            values.add(activityMap.getOrDefault(label, 0L));
        }

        trends.put("labels", labels);
        trends.put("values", values);

        return trends;
    }

    // 프로젝트 상태 데이터 조회 (차트용)
    public Map<String, Object> getProjectStatusData() {
        Map<String, Object> statusData = new HashMap<>();

        // 프로젝트 상태별 카운트를 위한 맵 초기화
        Map<ProjectStatus, Long> statusCounts = new HashMap<>();
        for (ProjectStatus status : ProjectStatus.values()) {
            statusCounts.put(status, projectRepository.countByStatus(status));
        }

        List<String> labels = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        List<String> colors = Arrays.asList(
                "rgba(108, 117, 125, 0.7)",  // 회색 (계획 중 - PLANNING)
                "rgba(0, 123, 255, 0.7)",    // 파랑 (진행 중 - IN_PROGRESS)
                "rgba(40, 167, 69, 0.7)",    // 초록 (완료 - COMPLETED)
                "rgba(255, 193, 7, 0.7)",    // 노랑 (보류 - ON_HOLD)
                "rgba(220, 53, 69, 0.7)"     // 빨강 (취소 - CANCELLED)
        );

        // 상태별 레이블과 카운트 추가
        for (Map.Entry<ProjectStatus, Long> entry : statusCounts.entrySet()) {
            ProjectStatus status = entry.getKey();
            Long count = entry.getValue();

            // 한글 레이블로 변환
            String label;
            switch (status) {
                case PLANNING:
                    label = "계획 중";
                    break;
                case IN_PROGRESS:
                    label = "진행 중";
                    break;
                case COMPLETED:
                    label = "완료";
                    break;
                case ON_HOLD:
                    label = "보류";
                    break;
                case CANCELLED:
                    label = "취소";
                    break;
                default:
                    label = status.name();
                    break;
            }

            labels.add(label);
            counts.add(count);
        }

        statusData.put("labels", labels);
        statusData.put("counts", counts);
        statusData.put("colors", colors);

        return statusData;
    }

    // 멤버 수 기준 상위 프로젝트 조회
    public List<Map<String, Object>> getTopProjectsByMemberCount(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 프로젝트 멤버 수를 기준으로 상위 N개 프로젝트 조회
        List<Object[]> topProjects = projectMemberRepository.findTopProjectsByMemberCount(limit);

        for (Object[] row : topProjects) {
            Map<String, Object> projectInfo = new HashMap<>();
            Long projectId = (Long) row[0];
            String projectName = (String) row[1];
            Long memberCount = (Long) row[2];

            projectInfo.put("projectId", projectId);
            projectInfo.put("projectName", projectName);
            projectInfo.put("memberCount", memberCount);
            result.add(projectInfo);
        }

        return result;
    }

    // 프로젝트 생성 추세 조회 - 개선된 버전
    public Map<String, Object> getProjectCreationTrends(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Map<String, Object> trends = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 날짜 범위에 대한 모든 날짜 레이블과 카운트 초기화
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        // startDateTime부터 endDateTime까지의 모든 날짜에 대한 기본 구조 생성
        LocalDateTime currentDate = startDateTime.toLocalDate().atStartOfDay();
        while (!currentDate.isAfter(endDateTime)) {
            labels.add(currentDate.format(formatter));
            values.add(0L); // 기본값은 0
            currentDate = currentDate.plusDays(1);
        }

        // 날짜가 없으면 기본값으로 오늘 하나만 표시
        if (labels.isEmpty()) {
            labels.add(LocalDateTime.now().format(formatter));
            values.add(0L);
        }

        // DB에서 데이터 조회
        List<Object[]> creationData = projectRepository.countProjectsCreatedAfter(startDateTime);

        // DB 데이터를 날짜별로 매핑하기 위한 맵 생성
        Map<String, Long> creationDataMap = new HashMap<>();

        if (creationData != null && !creationData.isEmpty()) {
            for (Object[] row : creationData) {
                try {
                    java.sql.Date date = (java.sql.Date) row[0];
                    Long count = (Long) row[1];
                    String dateStr = date.toLocalDate().format(formatter);
                    creationDataMap.put(dateStr, count);
                } catch (Exception e) {
                    log.error("프로젝트 생성 추세 데이터 처리 중 오류 발생: {}", e.getMessage());
                }
            }
        }

        // 조회된 데이터로 values 업데이트
        for (int i = 0; i < labels.size(); i++) {
            String dateLabel = labels.get(i);
            if (creationDataMap.containsKey(dateLabel)) {
                values.set(i, creationDataMap.get(dateLabel));
            }
        }

        trends.put("labels", labels);
        trends.put("values", values);

        return trends;
    }

    // 게시판별 활동 유형 통계
    // 게시판별 활동 유형 통계
    public Map<String, Object> getBoardActionTypeStats(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Map<String, Object> result = new HashMap<>();

        // 게시판 유형 목록 가져오기
        List<String> boardTypes = getBoardTypes();

        // 액션 유형 목록
        List<String> actionTypes = getActionTypes();

        // 게시판별 통계 데이터
        Map<String, Map<String, Long>> boardStats = new HashMap<>();

        // 액션 유형별 합계
        Map<String, Long> actionTotals = new HashMap<>();
        for (String actionType : actionTypes) {
            actionTotals.put(actionType, 0L);
        }

        // 모든 액션 유형 목록을 수집 (DB에서 실제로 사용된 액션 유형도 포함)
        Set<String> allActionTypes = new HashSet<>(actionTypes);

        // 각 게시판 유형별로 활동 유형 통계 수집
        for (String boardType : boardTypes) {
            Map<String, Long> actionCounts = new HashMap<>();

            // 해당 게시판의 활동 유형별 카운트 조회
            List<Object[]> stats = logBoardRepository.countByBoardNmAndActionGroupBy(boardType, startDateTime, endDateTime);

            for (Object[] row : stats) {
                String actionType = (String) row[0];
                Long count = (Long) row[1];

                // 알 수 없는 액션 유형도 목록에 추가
                allActionTypes.add(actionType);

                actionCounts.put(actionType, count);

                // 액션 유형별 합계 누적
                // actionTotals에 해당 액션이 없으면 먼저 초기화
                if (!actionTotals.containsKey(actionType)) {
                    actionTotals.put(actionType, 0L);
                }
                actionTotals.put(actionType, actionTotals.get(actionType) + count);
            }

            // 모든 액션 유형에 대해 카운트가 없는 경우 0으로 설정
            for (String action : allActionTypes) {
                if (!actionCounts.containsKey(action)) {
                    actionCounts.put(action, 0L);
                }
            }

            boardStats.put(boardType, actionCounts);
        }

        // 모든 액션 유형을 결과에 포함
        List<String> updatedActionTypes = new ArrayList<>(allActionTypes);

        // 결과 맵에 데이터 추가
        result.put("boardStats", boardStats);
        result.put("actionTypes", updatedActionTypes);
        result.put("actionTotals", actionTotals);

        // 각 액션 유형의 전체 합계 계산
        long totalActions = 0;
        for (Long count : actionTotals.values()) {
            totalActions += count;
        }
        result.put("totalActions", totalActions);

        return result;
    }

    // 일별 활동 유형 추세
    public Map<String, Object> getDailyActionTypeStats(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Map<String, Object> result = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        // 액션 유형 목록
        List<String> actionTypes = getActionTypes();
        result.put("actionTypes", actionTypes);

        // 날짜 레이블 준비
        List<String> dateLabels = new ArrayList<>();
        LocalDateTime currentDate = startDateTime;
        while (!currentDate.isAfter(endDateTime)) {
            dateLabels.add(currentDate.format(formatter));
            currentDate = currentDate.plusDays(1);
        }
        result.put("dates", dateLabels);

        // 각 액션 유형별 일별 통계 수집
        Map<String, List<Long>> actionData = new HashMap<>();

        // 일별 합계 데이터
        List<Long> dailyTotals = new ArrayList<>();
        for (int i = 0; i < dateLabels.size(); i++) {
            dailyTotals.add(0L);
        }

        for (String actionType : actionTypes) {
            List<Long> counts = new ArrayList<>();

            // 각 날짜별로 해당 액션의 카운트 조회
            for (int i = 0; i < dateLabels.size(); i++) {
                String dateLabel = dateLabels.get(i);
                LocalDateTime dayStart = LocalDateTime.parse(
                        LocalDateTime.now().getYear() + "-" + dateLabel + " 00:00:00",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                LocalDateTime dayEnd = dayStart.plusDays(1);

                Long count = logBoardRepository.countByActionAndActionDateBetween(actionType, dayStart, dayEnd);
                counts.add(count);

                // 일별 합계에 추가
                dailyTotals.set(i, dailyTotals.get(i) + count);
            }

            actionData.put(actionType, counts);
        }

        result.put("actionData", actionData);
        result.put("dailyTotals", dailyTotals);

        return result;
    }

}