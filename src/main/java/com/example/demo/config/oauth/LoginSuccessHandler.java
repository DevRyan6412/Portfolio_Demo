package com.example.demo.config.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // 세션에 사용자 정보 저장
        Object principal = authentication.getPrincipal();
        String username = (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();
        request.getSession().setAttribute("username", username);
        request.getSession().setAttribute("email", username);  // 일반적으로 username이 email임

        super.onAuthenticationSuccess(request, response, authentication);
    }

    @Override//RYAN [부트스트랩 템플릿 적용후] 로그인 시 실패한 Json요청으로 리다이렉트 되는현상때문에 추가함
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        // 부모 클래스의 저장된 URL을 가져옵니다.
        String targetUrl = super.determineTargetUrl(request, response);
        if (targetUrl != null) {
            // 쿼리 파라미터가 'continue'를 포함하면 무조건 기본 경로로 리다이렉트
            if (targetUrl.contains("continue")) {
                return "/";
            }
            // 쿼리 파라미터를 제거합니다.
            String cleanUrl = targetUrl.split("\\?")[0];
            // URL이 정적 리소스(여기서는 .css, .js 등)인 경우 기본 경로로 리다이렉트
            if (cleanUrl.endsWith(".css") || cleanUrl.endsWith(".js") ||
                    cleanUrl.endsWith(".png") || cleanUrl.endsWith(".jpg") ||
                    cleanUrl.endsWith(".jpeg") || cleanUrl.endsWith(".gif") ||
                    cleanUrl.endsWith(".woff") || cleanUrl.endsWith(".woff2") ||
                    cleanUrl.endsWith(".ttf")) {
                return "/";
            }
        }
        return targetUrl;
    }

}