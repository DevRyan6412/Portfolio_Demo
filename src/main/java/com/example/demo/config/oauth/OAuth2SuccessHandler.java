package com.example.demo.config.oauth;

import com.example.demo.domain.dto.OAuth2UserInfo;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.OAuth2Service;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    @Lazy
    private final OAuth2Service oAuth2Service;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Provider 판별
        String provider = determineProvider(request);
        log.debug("Provider determined: {}", provider);

        // Provider별 이메일 추출
        String email = extractEmail(attributes, provider);
        if (email == null) {
            log.error("Email not found in OAuth2User attributes: {}", attributes);
            throw new RuntimeException("OAuth2 인증에서 이메일 정보를 가져올 수 없습니다.");
        }
        log.debug("Extracted email: {}", email);

        if (isRegisteredUser(email)) {
            log.info("Existing user logged in: {}", email);

            // 기존 사용자 정보 조회
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            // OAuth2User 정보 업데이트
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

            // provider별 attributes와 nameAttributeKey 설정
            Map<String, Object> updatedAttributes = attributes;
            String nameAttributeKey;

            if ("naver".equals(provider)) {
                nameAttributeKey = "id";
                updatedAttributes = (Map<String, Object>) attributes.get("response");
            } else if ("kakao".equals(provider)) {
                nameAttributeKey = "id";
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                if (kakaoAccount != null) {
                    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                    if (profile != null) {
                        updatedAttributes = new HashMap<>();
                        updatedAttributes.put("id", attributes.get("id"));
                        updatedAttributes.put("email", kakaoAccount.get("email"));
                        updatedAttributes.put("nickname", profile.get("nickname"));
                    }
                }
            } else {  // google
                nameAttributeKey = "sub";
            }

            OAuth2User updatedOAuth2User = new DefaultOAuth2User(
                    Collections.singleton(authority),
                    updatedAttributes,
                    nameAttributeKey
            );

            // 새로운 Authentication 객체 생성
            OAuth2AuthenticationToken newAuth = new OAuth2AuthenticationToken(
                    updatedOAuth2User,
                    Collections.singleton(authority),
                    provider
            );

            // SecurityContext 업데이트
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            // 세션에 사용자 정보 저장
            request.getSession().setAttribute("username", user.getName());
            request.getSession().setAttribute("email", email);

            // 저장된 요청 URL이 있으면 해당 URL로 리다이렉트
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        // 새로운 사용자 처리
        OAuth2UserInfo userInfo = (OAuth2UserInfo) request.getSession().getAttribute("userInfo");
        if (userInfo == null) {
            log.info("Creating new OAuth2UserInfo for: {}", email);
            userInfo = createOAuth2UserInfo(attributes, provider);
        }

        // 토큰 생성 및 저장
        String token = UUID.randomUUID().toString();
        oAuth2Service.storeTemporaryUserInfo(token, userInfo);
        log.info("Stored temporary user info with token for email: {}", email);

        // URL 인코딩하여 리다이렉트
        String redirectUrl = "/oauth2/signup?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String extractEmail(Map<String, Object> attributes, String provider) {
        String email = null;
        switch (provider) {
            case "naver":
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                email = response != null ? (String) response.get("email") : null;
                break;
            case "kakao":
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
                break;
            case "google":
                email = (String) attributes.get("email");
                break;
        }
        log.debug("Extracted email for provider {}: {}", provider, email);
        return email;
    }

    private OAuth2UserInfo createOAuth2UserInfo(Map<String, Object> attributes, String provider) {
        String email = extractEmail(attributes, provider);
        String name = extractName(attributes, provider);
        String providerId = extractProviderId(attributes, provider);

        return OAuth2UserInfo.builder()
                .email(email)
                .name(name)
                .provider(provider)
                .providerId(providerId)
                .build();
    }

    private String extractName(Map<String, Object> attributes, String provider) {
        switch (provider) {
            case "naver":
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                return response != null ? (String) response.get("name") : null;
            case "kakao":
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                if (kakaoAccount != null) {
                    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                    return profile != null ? (String) profile.get("nickname") : null;
                }
                return null;
            case "google":
                return (String) attributes.get("name");
            default:
                return null;
        }
    }

    private String extractProviderId(Map<String, Object> attributes, String provider) {
        switch (provider) {
            case "naver":
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                return response != null ? (String) response.get("id") : null;
            case "kakao":
                return String.valueOf(attributes.get("id"));
            case "google":
                return (String) attributes.get("sub");
            default:
                return null;
        }
    }

    private String determineProvider(HttpServletRequest request) {
        String uri = request.getRequestURI().toLowerCase();
        if (uri.contains("google")) return "google";
        if (uri.contains("naver")) return "naver";
        if (uri.contains("kakao")) return "kakao";
        return "unknown";
    }

    private boolean isRegisteredUser(String email) {
        log.debug("Checking if user exists with email: {}", email);
        boolean exists = userRepository.existsByEmail(email);
        log.debug("User exists with email {}: {}", email, exists);
        return exists;
    }
}