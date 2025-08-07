package com.example.demo.controller;

import com.example.demo.domain.dto.OAuth2SignUpRequest;
import com.example.demo.domain.dto.OAuth2UserInfo;
import com.example.demo.domain.entity.User;
import com.example.demo.service.OAuth2Service;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final OAuth2Service oAuth2Service;

    @GetMapping("/signup")
    public String showSignupForm(@RequestParam(required = false) String token, HttpSession session, Model model) {
        if (token != null && !token.trim().isEmpty()) {
            // 토큰이 URL 파라미터로 전달된 경우
            OAuth2UserInfo userInfo = oAuth2Service.getUserInfoFromToken(token);
            if (userInfo != null) {
                model.addAttribute("email", userInfo.getEmail());
                model.addAttribute("name", userInfo.getName());
                model.addAttribute("provider", userInfo.getProvider());
                model.addAttribute("token", token);  // 토큰을 모델에 추가
                return "oauth2-signup";
            }
        }

        // 세션에서 userInfo를 확인
        OAuth2UserInfo userInfo = (OAuth2UserInfo) session.getAttribute("userInfo");
        if (userInfo == null) {
            throw new RuntimeException("사용자 정보가 없습니다.");
        }

        model.addAttribute("email", userInfo.getEmail());
        model.addAttribute("name", userInfo.getName());
        model.addAttribute("provider", userInfo.getProvider());
        model.addAttribute("token", token);  // 토큰을 모델에 추가

        return "oauth2-signup";
    }

    @PostMapping("/signup")
    public ResponseEntity<?> completeSignup(@Valid @RequestBody OAuth2SignUpRequest request) {
        try {
            // 토큰 유효성 검사 추가
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "토큰이 제공되지 않았습니다."
                ));
            }

            log.info("Received Signup Request with token: {}", request.getToken());
            User savedUser = oAuth2Service.completeSignup(request);
            log.info("Signup successful for user: {}", savedUser);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "회원가입이 완료되었습니다.",
                    "user", savedUser,
                    "logout", true

            ));
        } catch (IllegalArgumentException e) {
            log.error("Signup failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Signup failed due to an unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "회원가입 중 문제가 발생했습니다."
            ));
        }
    }
}