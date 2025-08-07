package com.example.demo.controller;

import com.example.demo.domain.entity.Invitation;
import com.example.demo.domain.entity.Project;
import com.example.demo.domain.entity.ProjectMember;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.InvitationRepository;
import com.example.demo.repository.ProjectMemberRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.InvitationService;
import com.example.demo.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/projects/{projectId}/management/invitations")
@RequiredArgsConstructor
public class InvitationController {
    private final InvitationService invitationService;
    private final EmailService emailService;       // 이메일 전송 서비스 (예시)
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    //초대 생성 및 이메일 전송 처리
    //요청 후 생성된 초대 정보를 view에 전달
    @PostMapping("/create")
    public String createInvitation(@RequestParam("emails") String emails,
                                   @PathVariable("projectId") Long projectId,
                                   Model model) {
        // 프로젝트 조회 (해당 프로젝트가 존재하는지 확인)
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AccessDeniedException("프로젝트를 찾을 수 없습니다."));

        // 전달된 이메일들을 콤마로 분리하여 처리
        String[] emailArr = emails.split(",");
        List<Invitation> invitations = new ArrayList<>();

        for (String email : emailArr) {
            email = email.trim();
            if (!email.isEmpty()) {
                Invitation invitation = invitationService.createInvitation(email, project);
                emailService.sendInvitationEmail(invitation, project);
                invitations.add(invitation);
            }
        }

        model.addAttribute("invitation", invitations);
        model.addAttribute("message", "초대가 생성되었습니다.");
        return "redirect:/projects/" + projectId + "/management"; // 초대 생성 완료 후 이동할 뷰 이름
    }

    //초대수락
    @GetMapping("/accept")
    public String acceptInvitation(@RequestParam("token") String token,
                                   @PathVariable("projectId") Long projectId,
                                   Model model,
                                   Authentication authentication) {
        // 현재 로그인한 사용자의 이메일 추출
        String currentEmail;
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            currentEmail = (String) oauth2User.getAttribute("email");
        } else {
            currentEmail = authentication.getName();
        }

        // 초대 수락 처리 (InvitationService 내부에서 ProjectMember 생성 등)
        Invitation invitation = invitationService.acceptInvitation(token);

        // 초대받은 이메일과 현재 로그인한 이메일이 일치하는지 검증
        if (!invitation.getEmail().equalsIgnoreCase(currentEmail)) {
            // 이메일 불일치 시 예외를 던져 기본 에러 페이지(Whitelabel Error Page)가 뜨도록 함
            throw new IllegalStateException("초대받은 계정으로 로그인하지 않으셨습니다. 초대받은 계정으로 로그인 후 시도해주세요.");
        }

        return "redirect:/projects/" + projectId;
    }


    //팀원 추방 (Leader만 사용 가능)[InvitationService에 구현됨 kickMember]
    @PostMapping("/kickMember")
    public String kickMember(@PathVariable("projectId") Long projectId,
                             @RequestParam("userId") Long userId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        // 현재 로그인한 사용자 이메일 추출
        String email;
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            email = (String) oauth2User.getAttribute("email");
        } else {
            email = authentication.getName();
        }

        // 현재 로그인한 User 객체 조회
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다: " + email));

        try {
            // InvitationService의 kickMember 메서드 호출
            invitationService.kickMember(projectId, userId, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "팀원 추방이 완료되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/projects/" + projectId + "/management";
    }

}
