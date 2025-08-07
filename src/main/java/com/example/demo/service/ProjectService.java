package com.example.demo.service;

import com.example.demo.domain.dto.ProjectRequest;
import com.example.demo.domain.entity.*;
import com.example.demo.repository.NoticeBoardRepository;
import com.example.demo.repository.ProjectMemberRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Log4j2
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final NoticeBoardRepository noticeBoardRepository;
    private final LogBoardService logBoardSerivce;

    // 프로젝트 생성
    @Transactional
    public Project createProject(ProjectRequest request, User creator) {
        System.out.println("=== 프로젝트 생성 디버깅 ===");
        System.out.println("Creator: " + creator);
        System.out.println("Creator ID: " + (creator != null ? creator.getId() : "null"));
        System.out.println("Creator Email: " + (creator != null ? creator.getEmail() : "null"));

        if (creator == null) {
            throw new IllegalArgumentException("프로젝트 생성자 정보가 없습니다.");
        }

        Project project = new Project();
        project.setProjectName(request.getProjectName());
        project.setDescription(request.getDescription());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        project.setStartDate(LocalDate.parse(request.getStartDate(), formatter).atStartOfDay());
        project.setEndDate(LocalDate.parse(request.getEndDate(), formatter).atStartOfDay());

        project.setCreator(creator);
        project.setStatus(ProjectStatus.PLANNING);
        //project.getMembers().add(creator); //기존의 Member 설정로직

        // ProjectMember 엔티티를 생성하여 추가
        ProjectMember projectMember = new ProjectMember();
        projectMember.setUser(creator);
        // 프로젝트 생성자를 Leader로 설정
        projectMember.setProjectRole(ProjectRole.Leader);
        // 편의 메소드를 통해 양방향 연관관계 설정
        project.addProjectMember(projectMember);

        // 추가된 ProjectMember의 정보를 로그로 출력하여 확인
        if (projectMember.getUser() != null) {
            System.out.println("ProjectMember 저장 정보:");
            System.out.println(" - User ID: " + projectMember.getUser().getId());
            System.out.println(" - User Email: " + projectMember.getUser().getEmail());
        } else {
            System.out.println("ProjectMember의 User 정보가 null입니다.");
        }
        //기본 게시판 생성
        createDefaultBoards(project);

        return project;
//        return projectRepository.save(project);
    }

    // 기본 게시판 생성 메소드 (서비스 계층에 위치)
    private void createDefaultBoards(Project project) {
        // Project를 먼저 저장하여 영속화
        project = projectRepository.save(project);
        // 기본 NoticeBoard 생성
        NoticeBoard defaultNotice = new NoticeBoard(project,
                "ADMIN",
                "admin@admin.com",
                "공지사항 게시판 이용규칙",
                "추후 작성");
        // NoticeBoard를 DB에 저장하여 ID를 할당받음
        defaultNotice = noticeBoardRepository.save(defaultNotice);
        project.getNoticeBoards().add(defaultNotice);

        // 기본 LogBoard 생성 (NoticeBoard의 ID를 사용)
        LogBoard defaultLog = LogBoard.builder()
                .project(project)
                .boardNm(defaultNotice.getBoardNm())   // 예: "noticeboard"
                .postId(defaultNotice.getId())           // 저장 후 할당된 NoticeBoard의 ID
                .action("ADD")                           // 초기 로그 액션
                .cName(defaultNotice.getCName())         // NoticeBoard에서 설정한 작성자 이름
                .actionDate(LocalDateTime.now())         // 현재 시간
                .build();
        project.getLogBoards().add(defaultLog);//자동 Cascade 저장방식
    }

    // [신규] @PreAuthorize 로 ProjectSecurity를 거쳐 ProjectMember인지 검증(Url로 접근 불가능)
    @PreAuthorize("@projectSecurity.hasAccessToProject(#id, principal)")
    public Project getProject(Long id, User user) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다."));

        // 로그 추가: 프로젝트에 저장된 멤버들의 정보 출력(유저 검증시 활용데이터 확인용 로그)
        project.getProjectMembers().forEach(pm -> {
            System.out.println("ProjectMember user id: " + pm.getUser().getId()
                    + ", email: " + pm.getUser().getEmail());
            System.out.println("ProjectMember user: " + (pm.getUser() != null ? pm.getUser().getId() : "null"));
        });
        System.out.println("현재 인증된 User id: " + user.getId() + ", email: " + user.getEmail());
        // 로그 종료(유저 검증시 활용데이터 확인용 로그)

        // 현재 사용자가 프로젝트의 멤버인지 검사 (생성자도 포함)
        boolean isMember = project.getProjectMembers().stream()
                .filter(pm -> pm.getUser() != null)
                .anyMatch(pm -> pm.getUser().getId() != null && pm.getUser().getId().equals(user.getId()));
        if (!isMember) {
            throw new AccessDeniedException("해당 프로젝트에 접근할 권한이 없습니다.");
        }
        return project;
    }


    // 사용자의 프로젝트 목록 조회
    public List<Project> getUserProjects(User user) {
        return projectRepository.findByProjectMembersUser(user);
    }

    // 현재 로그인한 사용자가 속한 프로젝트 목록 조회 (문자열 Email 기반)
    public List<Project> getProjectsForUser(String currentUserId) {
        // currentUserId를 이용해 User 엔티티를 조회 (없으면 빈 리스트 반환)
        User user = userRepository.findByEmail(currentUserId).orElse(null);
        if (user == null) {
            return List.of();
        }
        return getUserProjects(user);
    }

    // 관리자용 전체 프로젝트 목록 조회
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // 프로젝트 삭제를 위한 ID로 조회 (없으면 null 반환)
    public Project getProjectById(Long id) {
        return projectRepository.findById(id).orElse(null);
    }


    // 프로젝트 수정(ProjectMember-ProjectRole)
    @PreAuthorize("@projectSecurity.hasAccessToProject(#id, principal)")
    @Transactional
    public Project updateProject(Long id, ProjectRequest request, User user) {
        Project project = getProject(id, user);

        // 프로젝트 내에서 해당 사용자의 ProjectMember 엔티티 조회
        Optional<ProjectMember> projectMemberOpt = project.getProjectMembers().stream()
                .filter(pm -> pm.getUser().equals(user))
                .findFirst();

        // 권한 체크: 프로젝트 생성자거나, 해당 프로젝트 내 역할이 Leader 혹은 Admin 인 경우 수정 허용
        boolean isCreator = project.getCreator().equals(user);
        boolean hasProjectRole = projectMemberOpt.isPresent() &&
                (projectMemberOpt.get().getProjectRole() == ProjectRole.Leader ||
                        projectMemberOpt.get().getProjectRole() == ProjectRole.Admin);

        if (!isCreator && !hasProjectRole) {
            throw new AccessDeniedException("프로젝트를 수정할 권한이 없습니다.");
        }
        // 업데이트 전에 버전 체크
        Long currentVersion = project.getVersion(); // 현재 프로젝트의 버전
        Project projectToUpdate = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다."));

        if (!projectToUpdate.getVersion().equals(currentVersion)) {
            throw new OptimisticLockingFailureException("프로젝트를 수정하는 동안 버전 충돌이 발생했습니다. 다시 시도하십시오.");
        }

        // 프로젝트 정보 수정
        project.setProjectName(request.getProjectName());
        project.setDescription(request.getDescription());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        project.setStartDate(LocalDate.parse(request.getStartDate(), formatter).atStartOfDay());
        project.setEndDate(LocalDate.parse(request.getEndDate(), formatter).atStartOfDay());

        project.setStatus(request.getStatus());

        // 프로젝트 저장
        return projectRepository.save(project);
    }

    //프로젝트 멤버 권한 수정
    @Transactional
    public void updateProjectMemberRole(Long projectId, Long userId, ProjectRole newRole) {
        // 프로젝트 내 특정 멤버를 조회
        Optional<ProjectMember> optMember = projectMemberRepository.findByProjectIdAndUserId(projectId, userId);
        if (optMember.isEmpty()) {
            throw new IllegalArgumentException("해당 프로젝트 멤버를 찾을 수 없습니다.");
        }
        ProjectMember member = optMember.get();
        member.setProjectRole(newRole);
        projectMemberRepository.save(member);
    }


    // 프로젝트 삭제(ProjectMember-ProjectRole)
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    @Transactional
    public void deleteProject(Long projectId, User user) {
        Project project = getProject(projectId, user);

        // 프로젝트 생성자 여부 확인
        boolean isCreator = project.getCreator().equals(user);

        // 프로젝트 내에서 현재 사용자의 역할 확인
        Optional<ProjectMember> projectMemberOpt = project.getProjectMembers().stream()
                .filter(pm -> pm.getUser().equals(user))
                .findFirst();

        boolean hasProjectRole = projectMemberOpt.isPresent() &&
                (projectMemberOpt.get().getProjectRole() == ProjectRole.Admin ||
                        projectMemberOpt.get().getProjectRole() == ProjectRole.Leader);

        // 권한 체크: 생성자이거나 프로젝트 내에서 Leader 혹은 Admin 역할인 경우에만 삭제 허용
        if (!isCreator && !hasProjectRole) {
            throw new AccessDeniedException("프로젝트를 삭제할 권한이 없습니다.");
        }

        // 낙관적 락을 사용한 버전 체크 및 삭제
        Long currentVersion = project.getVersion();  // 현재 프로젝트 버전
        try {
            // 삭제 전에 버전 정보 확인
            Project projectToDelete = projectRepository.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다."));

            // 삭제된 프로젝트의 버전 정보 확인
            if (!projectToDelete.getVersion().equals(currentVersion)) {
                throw new OptimisticLockingFailureException("프로젝트 삭제 중 버전 충돌이 발생했습니다. 다시 시도하십시오.");
            }

            // 프로젝트 삭제
            projectRepository.delete(project);
        } catch (ObjectOptimisticLockingFailureException ex) {
            // 버전 충돌 예외 처리
            throw new OptimisticLockingFailureException("프로젝트를 삭제하는 동안 충돌이 발생했습니다. 다시 시도하십시오.");
        }
    }

    // 프로젝트 나가기(팀원)
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    @Transactional
    public void leaveProject(Long projectId, User user) {
        Project project = getProject(projectId, user);
        if (project == null) {
            throw new EntityNotFoundException("프로젝트를 찾을 수 없습니다.");
        }

        // 프로젝트 생성자는 탈퇴할 수 없도록 처리
        if (project.getCreator().equals(user)) {
            throw new IllegalArgumentException("프로젝트 생성자는 탈퇴할 수 없습니다.");
        }

        // 해당 프로젝트에서 사용자의 ProjectMember 엔티티 찾기
        Optional<ProjectMember> memberOpt = project.getProjectMembers().stream()
                .filter(pm -> pm.getUser().equals(user))
                .findFirst();

        if (memberOpt.isPresent()) {
            ProjectMember projectMember = memberOpt.get();
            // Project 엔티티의 removeProjectMember 메소드를 이용해 ProjectMember에서 제거
            project.removeProjectMember(projectMember);
            // 명시적으로 repository에서 삭제하고 flush하여 DB에 반영
            projectMemberRepository.delete(projectMember);
            projectMemberRepository.flush();
        } else {
            throw new IllegalArgumentException("해당 사용자는 이 프로젝트의 멤버가 아닙니다.");
        }
    }

    // 리더로 참여한 프로젝트 조회
    public List<Project> getProjectsByLeader(String userEmail) {
        return projectMemberRepository.findProjectsByUserAndRole(userEmail, ProjectRole.Leader);
    }

    // 멤버로 참여한 프로젝트 조회
    public List<Project> getProjectsByMember(String userEmail) {
        return projectMemberRepository.findProjectsByUserAndRole(userEmail, ProjectRole.Member);
    }
    //Admin으로 참여한 프로젝트 조회
    public List<Project> getProjectsByAdmin(String userEmail) {
        return projectMemberRepository.findProjectsByUserAndRole(userEmail, ProjectRole.Admin);
    }
}