package com.example.demo.config.oauth;

import com.example.demo.domain.dto.OAuth2UserInfo;
import com.example.demo.service.TemporaryTokenStorage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final TemporaryTokenStorage tokenStorage;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();

        OAuth2UserInfo userInfo = extractUserInfo(registrationId, attributes);

        if (userInfo.getEmail() == null || userInfo.getName() == null) {
            throw new RuntimeException("필수 정보가 누락되었습니다: " + registrationId);
        }

        // 세션에만 저장하고 토큰 생성은 하지 않음
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        session.setAttribute("userInfo", userInfo);

        return oauth2User;
    }

    private OAuth2UserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> extractGoogleUserInfo(attributes);
            case "naver" -> extractNaverUserInfo(attributes);
            case "kakao" -> extractKakaoUserInfo(attributes);
            default -> throw new RuntimeException("지원하지 않는 인증 공급자입니다: " + registrationId);
        };
    }

    private OAuth2UserInfo extractGoogleUserInfo(Map<String, Object> attributes) {
        return OAuth2UserInfo.builder()
                .email((String) attributes.get("email"))
                .name((String) attributes.get("name"))
                .provider("google")
                .providerId((String) attributes.get("sub"))
                .build();
    }

    private OAuth2UserInfo extractNaverUserInfo(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return OAuth2UserInfo.builder()
                .email((String) response.get("email"))
                .name((String) response.get("name"))
                .provider("naver")
                .providerId((String) response.get("id"))
                .build();
    }

    private OAuth2UserInfo extractKakaoUserInfo(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuth2UserInfo.builder()
                .email((String) kakaoAccount.getOrDefault("email", "unknown@kakao.com"))
                .name((String) profile.get("nickname"))
                .provider("kakao")
                .providerId(String.valueOf(attributes.get("id")))
                .build();
    }

}
