package com.example.demo.controller;

import com.example.demo.domain.entity.Project;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@ControllerAdvice
public class GlobalController {

    @Autowired
    private UserRepository userRepository;

    // username 추출 로직을 별도의 메서드로 분리
    //th:text="${username}" 로 어디서든 유저이름값 사용 가능
    @ModelAttribute("username")
    public String extractUsername(Authentication authentication) {
        String username = "Unknown User"; // 기본값
        String email = null;

        if (authentication != null) {
            if (authentication.getPrincipal() instanceof OAuth2User) {
                // OAuth2 로그인의 경우
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                if (oauth2User.getAttribute("email") != null) { // Google
                    email = oauth2User.getAttribute("email");
                } else if (oauth2User.getAttribute("response") != null) { // Naver
                    Map<String, Object> response = (Map<String, Object>) oauth2User.getAttribute("response");
                    email = (String) response.get("email");
                } else if (oauth2User.getAttribute("kakao_account") != null) { // Kakao
                    Map<String, Object> kakaoAccount = (Map<String, Object>) oauth2User.getAttribute("kakao_account");
                    email = (String) kakaoAccount.get("email");
                }
            } else if (authentication.getPrincipal() instanceof UserDetails) {
                // 일반 로그인의 경우
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                email = userDetails.getUsername(); // 일반적으로 email을 username으로 사용
            }

            // 데이터베이스에서 사용자 정보 조회
            if (email != null) {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    username = user.getName();
                }
            }
        }
        return username;
    }
}
