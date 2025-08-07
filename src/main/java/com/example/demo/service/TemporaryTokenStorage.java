package com.example.demo.service;

import com.example.demo.domain.dto.OAuth2UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TemporaryTokenStorage {

    private final Map<String, TokenData> storage = new ConcurrentHashMap<>();
    private static final long DEFAULT_EXPIRATION_MINUTES = 30;

    // 토큰 저장
    public void store(String token, OAuth2UserInfo userInfo) {
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(DEFAULT_EXPIRATION_MINUTES);
        storage.put(token, new TokenData(userInfo, expirationTime));
        log.info("[STORE] Token stored: {} | Expiration: {} | UserInfo: {}", token, expirationTime, userInfo);
    }

    // 토큰 검색
    public OAuth2UserInfo get(String token) {
        log.info("[GET] Attempting to retrieve token: {}", token);

        TokenData tokenData = storage.get(token);

        if (tokenData == null) {
            log.warn("[GET] Token not found in storage: {}", token);
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        if (tokenData.expirationTime.isBefore(LocalDateTime.now())) {
            log.warn("[GET] Token expired: {} | Expiration Time: {}", token, tokenData.expirationTime);
            storage.remove(token); // 만료된 토큰 제거
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        log.info("[GET] Token successfully retrieved: {} | Expiration Time: {} | UserInfo: {}", token, tokenData.expirationTime, tokenData.userInfo);
        return tokenData.userInfo;
    }

    // 토큰 삭제
    public void remove(String token) {
        if (storage.containsKey(token)) {
            log.info("[REMOVE] Removing token: {}", token);
        } else {
            log.warn("[REMOVE] Attempted to remove non-existing token: {}", token);
        }
        storage.remove(token);
    }

    private static class TokenData {
        private final OAuth2UserInfo userInfo;
        private final LocalDateTime expirationTime;

        public TokenData(OAuth2UserInfo userInfo, LocalDateTime expirationTime) {
            this.userInfo = userInfo;
            this.expirationTime = expirationTime;
        }
    }
}