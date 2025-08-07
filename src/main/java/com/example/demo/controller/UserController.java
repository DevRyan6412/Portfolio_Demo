package com.example.demo.controller;

import com.example.demo.domain.dto.UserSignUpRequest;
import com.example.demo.domain.dto.UserResponse;
import com.example.demo.domain.dto.UserUpdateRequest;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @GetMapping("/users/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/api/users/signup")
    @ResponseBody
    public ResponseEntity<UserResponse> signUp(@Valid @RequestBody UserSignUpRequest request) {
        UserResponse response = userService.signUp(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/users/check-email")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        boolean isAvailable = userService.isEmailAvailable(email);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", isAvailable);
        return ResponseEntity.ok(response);
    }

@GetMapping({"/profile", "/projects/{projectId}/profile"})
public String showProfile(@PathVariable(name = "projectId", required = false) Long projectId,
                          @RequestParam(name="projectId", required=false) Long projectIdReq,
                          Model model,
                          Authentication authentication) {
    // 경로 변수와 쿼리 파라미터 중 하나를 우선 선택
    Long finalProjectId = (projectId != null) ? projectId : projectIdReq;
    if (authentication != null && authentication.isAuthenticated()) {
        UserResponse userResponse = userService.getUserInfo(authentication.getPrincipal());
        model.addAttribute("user", userResponse);
        model.addAttribute("projectId", finalProjectId);
        return "profile";
    }
    return "redirect:/login";
}

    @PostMapping({"/user/update", "/projects/{projectId}/user/update"})
    public String updateProfile(
            @PathVariable(name = "projectId", required = false) Long projectId,
            @RequestParam(name = "projectId", required = false) Long projectIdReq,
            @AuthenticationPrincipal OAuth2User oauth2User,
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute UserUpdateRequest updateRequest,
            RedirectAttributes redirectAttributes) {

        Long finalProjectId = (projectId != null) ? projectId : projectIdReq;

        try {
            UserUpdateRequest filteredRequest = new UserUpdateRequest();

            if (updateRequest.getName() != null && !updateRequest.getName().trim().isEmpty()) {
                filteredRequest.setName(updateRequest.getName().trim());
            }
            if (updateRequest.getPhoneNumber() != null && !updateRequest.getPhoneNumber().trim().isEmpty()) {
                filteredRequest.setPhoneNumber(updateRequest.getPhoneNumber().trim());
            }
            if (updateRequest.getAddress() != null && !updateRequest.getAddress().trim().isEmpty()) {
                filteredRequest.setAddress(updateRequest.getAddress().trim());
            }

            UserResponse updatedUser;
            if (oauth2User != null) {
                updatedUser = userService.updateOAuth2User(oauth2User, filteredRequest);
            } else if (userDetails != null) {
                updatedUser = userService.updateUser(userDetails.getUsername(), filteredRequest);
            } else {
                throw new IllegalArgumentException("User not authenticated");
            }

            redirectAttributes.addFlashAttribute("message", "프로필이 성공적으로 업데이트되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "프로필 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }

        // projectId가 있다면 -> /projects/{projectId}/profile
        // 없으면 -> /profile
        if (finalProjectId != null) {
            return "redirect:/projects/" + finalProjectId + "/profile";
        }
        return "redirect:/profile";
    }

    @PostMapping("/user/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        log.info("Password change requested for user: {}", userDetails.getUsername());
        try {
            // 1. 현재 비밀번호 확인
            if (!passwordEncoder.matches(currentPassword,
                    userRepository.findByEmail(userDetails.getUsername())
                            .orElseThrow()
                            .getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }

            // 2. 새 비밀번호 일치 확인
            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
            }

            // 3. 현재 비밀번호와 새 비밀번호가 같은지 확인
            if (passwordEncoder.matches(newPassword,
                    userRepository.findByEmail(userDetails.getUsername())
                            .orElseThrow()
                            .getPassword())) {
                throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
            }

            userService.changePassword(userDetails.getUsername(), newPassword);
            log.info("Password changed successfully for user: {}", userDetails.getUsername());
            redirectAttributes.addFlashAttribute("message", "비밀번호가 성공적으로 변경되었습니다.");
        } catch (Exception e) {
            log.error("Error changing password for user: {}", userDetails.getUsername(), e);
            redirectAttributes.addFlashAttribute("error", "비밀번호 변경 중 오류가 발생했습니다: " + e.getMessage());
        }
        return "redirect:/profile";
    }

@PostMapping({"/user/delete", "/projects/{projectId}/user/delete"})
public String deleteAccount(
        @PathVariable(name = "projectId", required = false) Long projectId,
        @RequestParam(name = "projectId", required = false) Long projectIdReq,
        @AuthenticationPrincipal OAuth2User oauth2User,
        @AuthenticationPrincipal UserDetails userDetails,
        HttpServletRequest request,
        RedirectAttributes redirectAttributes) {

    Long finalProjectId = (projectId != null) ? projectId : projectIdReq;
    try {
        if (oauth2User != null) {
            log.info("OAuth2 사용자 삭제 시도: {}", oauth2User.getName());
            userService.deleteOAuth2User(oauth2User);
            log.info("OAuth2 사용자 삭제 완료");
        } else if (userDetails != null) {
            log.info("일반 사용자 삭제 시도: {}", userDetails.getUsername());
            userService.deleteUser(userDetails.getUsername());
            log.info("일반 사용자 삭제 완료");
        } else {
            throw new IllegalArgumentException("User not authenticated");
        }

        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        redirectAttributes.addFlashAttribute("message", "탈퇴처리가 완료되었습니다.");
        return "redirect:/";
    } catch (Exception e) {
        log.error("회원 탈퇴 중 오류 발생: {}", (userDetails != null ? userDetails.getUsername() : "Unknown"), e);
        redirectAttributes.addFlashAttribute("error", "회원 탈퇴 처리 중 오류가 발생했습니다: " + e.getMessage());
        return "redirect:/logout";
     }
    }
}