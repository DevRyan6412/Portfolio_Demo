package com.example.demo.service;

import com.example.demo.domain.dto.UserSignUpRequest;
import com.example.demo.domain.dto.UserResponse;
import com.example.demo.domain.dto.UserUpdateRequest;
import com.example.demo.domain.entity.*;
import com.example.demo.repository.ProjectMemberRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;


    @Transactional
    public UserResponse signUp(UserSignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .address(request.getAddress())
                .phoneNumber(request.getPhoneNumber())
                .role(Role.USER)
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserInfo(Object principal) {
        if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            String email = oauth2User.getAttribute("email");
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            return UserResponse.from(user);
        } else if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            return UserResponse.from(user);
        } else {
            throw new IllegalArgumentException("Unsupported principal type");
        }
    }

    @Transactional
    public UserResponse updateUser(String email, UserUpdateRequest updateRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (updateRequest.getName() != null && !updateRequest.getName().trim().isEmpty()) {
            user.setName(updateRequest.getName());
        }
        if (updateRequest.getPhoneNumber() != null && !updateRequest.getPhoneNumber().trim().isEmpty()) {
            user.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        if (updateRequest.getAddress() != null && !updateRequest.getAddress().trim().isEmpty()) {
            user.setAddress(updateRequest.getAddress());
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateOAuth2User(OAuth2User oauth2User, UserUpdateRequest updateRequest) {
        String email = oauth2User.getAttribute("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (updateRequest.getName() != null && !updateRequest.getName().trim().isEmpty()) {
            user.setName(updateRequest.getName());
        }
        if (updateRequest.getPhoneNumber() != null && !updateRequest.getPhoneNumber().trim().isEmpty()) {
            user.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        if (updateRequest.getAddress() != null && !updateRequest.getAddress().trim().isEmpty()) {
            user.setAddress(updateRequest.getAddress());
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String email, String newPassword) {
        log.info("Starting password change for user: {}", email);

        // 1. 사용자 찾기
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        log.info("Found user with id: {}", user.getId());

        // 2. 새 비밀번호 유효성 검사
        if (!isPasswordValid(newPassword)) {
            throw new IllegalArgumentException("새 비밀번호는 최소 8자 이상이며, 문자, 숫자, 특수문자를 포함해야 합니다.");
        }

        // 3. 새 비밀번호 암호화 및 설정
        String encodedPassword = passwordEncoder.encode(newPassword);
        log.info("Password encrypted. New password hash: {}", encodedPassword);

        user.setPassword(encodedPassword);

        // 4. 사용자 정보 저장
        userRepository.save(user);
        log.info("User saved with new password.");

        // 5. 저장 후 확인
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        if (passwordEncoder.matches(newPassword, updatedUser.getPassword())) {
            log.info("Password change completed successfully");
        } else {
            log.error("Password change failed - verification error");
            throw new RuntimeException("Password change failed - verification error");
        }
    }

    private boolean isPasswordValid(String password) {
        String pattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
        boolean isValid = password.matches(pattern);
        log.info("Password validation result: {}", isValid);
        return isValid;
    }

    @Transactional
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        // 해당 사용자가 생성자로 등록된 모든 프로젝트 조회//RYAN
        List<Project> projects = projectRepository.findByCreator(user);

        // 관리자로 생성자 이전 및 프로젝트 설명 업데이트
//        User adminUser = getAdminUser(); // 관리자로 사용할 User 객체를 조회하는 메서드(Service에 메서드필요)
        User adminUser = userRepository.findByEmail("admin@admin.com")//관리자 직접 지정
                .orElseThrow(() -> new UsernameNotFoundException("Admin user not found with email: admin@admin.com"));
        for (Project project : projects) {
            project.setCreator(adminUser);
            // 예: 프로젝트 설명에 "생성자 이전됨" 문구 추가 (기존 설명 뒤에 추가)
            project.setDescription(project.getDescription() + " [생성자 이전됨]");
            projectRepository.save(project);
        }
        // 해당 사용자가 프로젝트 멤버로 참여 중인 ProjectMember들 재할당
        // 기존의 ProjectMember 엔티티는 삭제하고, 새로 생성하여 관리자로 재등록합니다.
        List<ProjectMember> memberships = new ArrayList<>(user.getProjectMemberships());
        for (ProjectMember pm : memberships) {
            // 기존 ProjectMember 제거
            // 먼저, 해당 프로젝트에서 제거
            pm.getProject().getProjectMembers().remove(pm);
            // 그리고, 삭제 처리 (별도의 repository나 entityManager를 이용해 삭제)
            projectMemberRepository.delete(pm);

            // 새 ProjectMember 엔티티 생성: 같은 프로젝트, 동일한 역할, 하지만 user를 adminUser로 설정
            ProjectMember newPm = new ProjectMember();
            newPm.setProject(pm.getProject());
            newPm.setUser(adminUser);
//            newPm.setProjectRole(pm.getProjectRole());
            newPm.setProjectRole(ProjectRole.Admin);
            // 새 엔티티를 저장
            projectMemberRepository.save(newPm);

            // 선택적으로, 해당 프로젝트의 projectMembers 컬렉션에 새 엔티티 추가
            pm.getProject().getProjectMembers().add(newPm);
        }//RYAN
        userRepository.delete(user);
    }

    @Transactional
    public void deleteOAuth2User(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        // 해당 사용자가 생성자로 등록된 모든 프로젝트 조회//RYAN
        List<Project> projects = projectRepository.findByCreator(user);

        // 관리자로 생성자 이전 및 프로젝트 설명 업데이트
//        User adminUser = getAdminUser(); // 관리자로 사용할 User 객체를 조회하는 메서드(메서드 필요)
        User adminUser = userRepository.findByEmail("admin@admin.com")//관리자 직접 지정
                .orElseThrow(() -> new UsernameNotFoundException("Admin user not found with email: admin@admin.com"));
        for (Project project : projects) {
            project.setCreator(adminUser);
            project.setDescription(project.getDescription() + " [생성자 이전됨]");
            projectRepository.save(project);
        }
        // 해당 사용자가 프로젝트 멤버로 참여 중인 ProjectMember들 재할당
        // 기존의 ProjectMember 엔티티는 삭제하고, 새로 생성하여 관리자로 재등록합니다.
        List<ProjectMember> memberships = new ArrayList<>(user.getProjectMemberships());
        for (ProjectMember pm : memberships) {
            // 기존 ProjectMember 제거
            // 먼저, 해당 프로젝트에서 제거
            pm.getProject().getProjectMembers().remove(pm);
            // 그리고, 삭제 처리 (별도의 repository나 entityManager를 이용해 삭제)
            projectMemberRepository.delete(pm);

            // 새 ProjectMember 엔티티 생성: 같은 프로젝트, 동일한 역할, 하지만 user를 adminUser로 설정
            ProjectMember newPm = new ProjectMember();
            newPm.setProject(pm.getProject());
            newPm.setUser(adminUser);
            newPm.setProjectRole(pm.getProjectRole());
            // 새 엔티티를 저장
            projectMemberRepository.save(newPm);

            // 선택적으로, 해당 프로젝트의 projectMembers 컬렉션에 새 엔티티 추가
            pm.getProject().getProjectMembers().add(newPm);
        }//RYAN
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public Page<User> getUsers(String keyword, Pageable pageable) {
        // keyword가 없으면 전체 조회, 있으면 검색
        if (keyword == null || keyword.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByNameContainingOrEmailContaining(keyword, keyword, pageable);
    }

    @Transactional
    public void adminDeleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        User adminUser = userRepository.findByEmail("admin@admin.com")
                .orElseThrow(() -> new UsernameNotFoundException("Admin user not found with email: admin@admin.com"));

        // 해당 사용자가 생성자로 등록된 모든 프로젝트 재할당
        List<Project> projectsAsCreator = projectRepository.findByCreator(user);
        for (Project project : projectsAsCreator) {
            project.setCreator(adminUser);
            project.setDescription(project.getDescription() + " [관리자 권한으로 삭제한 유저의 프로젝트입니다.]");
            projectRepository.save(project);
            log.info("프로젝트 '{}'의 생성자가 '{}'에서 '{}'로 변경됨", project.getProjectName(), user.getEmail(), adminUser.getEmail());
        }

        // 해당 유저의 프로젝트 멤버십 삭제 및 재할당
        List<ProjectMember> memberships = new ArrayList<>(user.getProjectMemberships());
        for (ProjectMember pm : memberships) {
            // 기존 멤버십 삭제
            pm.getProject().getProjectMembers().remove(pm);
            projectMemberRepository.delete(pm);

            // 새 ProjectMember 엔티티 생성: 같은 프로젝트, 동일한 역할, adminUser로 재할당
            ProjectMember newPm = new ProjectMember();
            newPm.setProject(pm.getProject());
            newPm.setUser(adminUser);
            newPm.setProjectRole(ProjectRole.Admin);
            projectMemberRepository.save(newPm);
            // 선택적으로, 프로젝트 컬렉션에도 추가
            pm.getProject().getProjectMembers().add(newPm);
        }

        // 최종적으로 유저 삭제
        userRepository.delete(user);
        log.info("사용자 '{}' 삭제 완료", email);
    }

    @Transactional
    public void updateUserRole(Long userId, String roleStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        // Role enum으로 변환 (예: Role.valueOf(roleStr))
        user.setRole(Role.valueOf(roleStr));
        userRepository.save(user);
    }

}