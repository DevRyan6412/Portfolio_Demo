package com.example.demo.config;

import com.example.demo.domain.entity.Project;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component("projectSecurity")
@RequiredArgsConstructor
public class ProjectSecurity {

    private final ProjectRepository projectRepository;

    public boolean hasAccessToProject(Long projectId, Object principal) {
        String email = null;

        // principal이 org.springframework.security.core.userdetails.User 인 경우 (폼 로그인)
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        }
        // principal이 OAuth2User인 경우 (소셜 로그인)
        else if (principal instanceof OAuth2User) {
            email = ((OAuth2User) principal).getAttribute("email");
        }
        // principal이 도메인 User인 경우 (커스텀 사용자 객체)
        else if (principal instanceof User) {
            email = ((User) principal).getEmail();
        }

        if(email == null) {
            throw new AccessDeniedException("사용자 이메일을 확인할 수 없습니다.");
        }

        // 프로젝트 조회
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AccessDeniedException("프로젝트를 찾을 수 없습니다."));

        final String userEmail = email;
        // 프로젝트 생성자 검증
        boolean isCreator = project.getCreator() != null &&
                project.getCreator().getEmail().equalsIgnoreCase(userEmail);

        // 프로젝트 멤버 검증
        boolean isMember = project.getProjectMembers().stream()
                .filter(pm -> pm.getUser() != null)
                .anyMatch(pm -> pm.getUser().getEmail().equalsIgnoreCase(userEmail));

        // 생성자이거나 멤버에 포함되어 있으면 접근 허용
        return isCreator || isMember;
    }
}
