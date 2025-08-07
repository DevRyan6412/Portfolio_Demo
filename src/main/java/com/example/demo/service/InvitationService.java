package com.example.demo.service;

import com.example.demo.domain.entity.*;
import com.example.demo.repository.InvitationRepository;
import com.example.demo.repository.ProjectMemberRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Log4j2
public class InvitationService {
    private final InvitationRepository invitationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService; // 프로젝트 조회 및 검증을 위해 추가
    private final LogBoardService logBoardService; // LogBoard 기록을 위한 서비스

    //초대 생성
    @Transactional
    public Invitation createInvitation(String email, Project project) {
        Invitation invitation = new Invitation();
        invitation.setEmail(email);
        invitation.setProject(project);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(3)); // 7일간 유효
        invitation.setStatus(InvitationStatus.PENDING);

        return invitationRepository.save(invitation);
    }

    //초대 수락
    //토큰을 이용하여 초대를 수락하고, ProjectMember 생성
    @Transactional
    public Invitation acceptInvitation(String token) {
        // 토큰으로 Invitation 조회
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대 토큰입니다."));

        // 만료 체크
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("초대가 만료되었습니다.");
        }

        // 초대 대상 이메일에 해당하는 사용자 조회
        User user = userRepository.findByEmail(invitation.getEmail())
                .orElseThrow(() -> new IllegalStateException("사용자 계정이 존재하지 않습니다."));

        // 중복 가입 여부 체크 후 ProjectMember 생성
        if (!projectMemberRepository.existsByProjectAndUser(invitation.getProject(), user)) {
            ProjectMember projectMember = new ProjectMember();
            projectMember.setProject(invitation.getProject());
            projectMember.setUser(user);
            projectMember.setProjectRole(ProjectRole.Member); // 기본 역할: MEMBER
            projectMemberRepository.save(projectMember);
        }

        // 초대 상태 업데이트
        invitation.setStatus(InvitationStatus.ACCEPTED);
        Invitation savedInvitation = invitationRepository.save(invitation);

        // LogBoard에 기록 남기기
        logBoardService.saveLog(
                invitation.getProject(),       // project
                "invitation",                  // boardNm
                savedInvitation.getId(),       // postId: 초대의 ID 사용 (필요에 따라 변경)
                user.getEmail(),               // createdBy
                user.getEmail(),               // modifiedBy
                "INVITATION",                  // action
                LocalDateTime.now(),           // actionDate
                user.getName()                 // cName (초대한 유저)
        );

        return savedInvitation;

    }

    @Transactional
    public void kickMember(Long projectId, Long userId, User currentUser) {
        // 프로젝트 조회 (보안 검증 포함)
        Project project = projectService.getProject(projectId, currentUser);
        log.info("프로젝트 조회됨: projectId={}, currentUserId={}", projectId, currentUser.getId());

        // 리더 권한 확인
        if (!project.getCreator().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("리더만 팀원을 추방할 수 있습니다.");
        }

        // 리더 자신은 추방 불가
        if (project.getCreator().getId().equals(userId)) {
            throw new IllegalArgumentException("리더는 추방할 수 없습니다.");
        }

        // 팀원 조회 후 삭제
        ProjectMember projectMember = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new IllegalArgumentException("팀원을 찾을 수 없습니다."));
        log.info("삭제할 팀원 엔티티 조회됨: {}", projectMember);

        // LogBoard에 기록
        // boardNm: "팀원 추방", postId: 추방 대상 사용자의 ID, action: "KICK"
        logBoardService.saveLog(
                project,                            // project
                "kick",                        // boardNm
                userId,                             // postId (추방 대상 사용자 ID)
                currentUser.getEmail(),             // createdBy (추방 요청자)
                currentUser.getEmail(),             // modifiedBy
                "KICK",                             // action
                LocalDateTime.now(),                // actionDate
                projectMember.getUser().getName()  // cName (추방 대상 사용자의 이메일)
        );

        // 양방향 연관관계 업데이트: Project 엔티티의 removeProjectMember 메서드 사용
        project.removeProjectMember(projectMember);

        // Repository를 통해 삭제 및 flush 처리
        projectMemberRepository.delete(projectMember);
        projectMemberRepository.flush();
        log.info("팀원 엔티티 삭제 요청 완료: projectId={}, userId={}", projectId, userId);
    }

}