package com.example.demo.service;

import com.example.demo.domain.dto.OAuth2SignUpRequest;
import com.example.demo.domain.dto.OAuth2UserInfo;
import com.example.demo.domain.entity.Role;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryTokenStorage tokenStorage;

    /**
     * 사용자 정보를 임시 저장소에 저장합니다.
     */
    public void storeTemporaryUserInfo(String token, OAuth2UserInfo userInfo) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Attempted to store user info with invalid token.");
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        log.info("Storing user info for token: {}", token);
        tokenStorage.store(token, userInfo);
    }


    /**
     * 토큰으로부터 사용자 정보를 가져옵니다.
     */
    public OAuth2UserInfo getUserInfoFromToken(String token) {
        log.info("Retrieving user info for token: {}", token);
        return tokenStorage.get(token);
    }

    /**
     * 회원가입을 완료합니다.
     */
    @Transactional
    public User completeSignup(OAuth2SignUpRequest request) {
        log.info("Starting signup process for token: {}", request.getToken());

        // 1. 토큰으로 사용자 정보 가져오기
        OAuth2UserInfo userInfo = tokenStorage.get(request.getToken());
        if (userInfo == null) {
            log.warn("No user info found for token: {}", request.getToken());


            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        // 2. 임시 비밀번호 생성 및 인코딩
        String tempPassword = UUID.randomUUID().toString();
        String encodedPassword = passwordEncoder.encode(tempPassword);

        // 3. 토큰 삭제 (중복 처리 방지)
        tokenStorage.remove(request.getToken());

        // 4. User 엔티티 생성
        User user = User.builder()
                .email(userInfo.getEmail())
                .name(userInfo.getName())
                .password(encodedPassword)
                .provider(request.getProvider())
                .providerId(userInfo.getProviderId())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .role(Role.USER) // 기본 사용자 권한
                .build();

        // 5. 저장 및 반환
        return userRepository.save(user);
    }
}
