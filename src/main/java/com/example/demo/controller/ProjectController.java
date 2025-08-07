package com.example.demo.controller;

import com.example.demo.domain.dto.ProjectRequest;
import com.example.demo.domain.entity.Project;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Log4j2
public class ProjectController {
    private final ProjectService projectService;
    private final UserRepository userRepository;

    // 프로젝트 생성 API
    @PostMapping
    public ResponseEntity<Project> createProject(
            @Valid @RequestBody ProjectRequest request) {

        // 보안 컨텍스트에서 사용자 정보 조회
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        System.out.println("=== 인증 정보 디버깅 ===");
        System.out.println("Authentication Object: " + authentication);
        System.out.println("Principal: " + authentication.getPrincipal());
        System.out.println("Principal Type: " + authentication.getPrincipal().getClass());
        System.out.println("Authentication Name: " + authentication.getName());

        // 익명 사용자인지 확인
        if (authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        // 사용자 조회
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        Project project = projectService.createProject(request, user);
        return ResponseEntity.ok(project);
    }

    // 프로젝트 조회 API (현재 사용자가 멤버인지 검증)
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id,
                                              @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.getProject(id, user));
    }

    // 사용자의 프로젝트 목록 조회 API
    @GetMapping("/my")
    public ResponseEntity<List<Project>> getUserProjects(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.getUserProjects(user));
    }

    // 프로젝트 수정 API
    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.updateProject(id, request, user));
    }

    // 프로젝트 삭제 API
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        projectService.deleteProject(id, user);
        return ResponseEntity.noContent().build();
    }
}
