package com.example.demo.config;

import com.example.demo.domain.dto.OAuth2UserInfo;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String>, ApplicationContextAware {

    private static ApplicationContext context;  // ApplicationContext 저장

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보가 없거나 익명 인증인 경우 Optional.empty() 반환
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        // 만약 principal이 OAuth2UserInfo 타입이라면 (직접 변환된 경우)
        if (principal instanceof OAuth2UserInfo) {
            String email = ((OAuth2UserInfo) principal).getEmail();
            if (email != null && !email.isEmpty()) {
                return Optional.of(email);
            }
        }

        // 만약 principal이 OAuth2User라면, attributes에서 name을 가져오기
        if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            Map<String, Object> attributes = oauth2User.getAttributes();
            Object emailObj = attributes.get("email");
            if (emailObj != null) {
                return Optional.of(emailObj.toString());
            }
        }

//        // 그 외의 경우, 기본적으로 authentication.getName()을 사용
        return Optional.of(authentication.getName());
    }
}
