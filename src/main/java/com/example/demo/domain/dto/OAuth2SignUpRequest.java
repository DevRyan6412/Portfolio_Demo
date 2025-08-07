package com.example.demo.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuth2SignUpRequest {
    @NotBlank(message = "토큰은 필수 항목입니다")
    private String token;       // 인증 토큰
    private String provider;    // 공급자 (google, naver, kakao)
    private String name;        // 사용자 이름
    private String email;       // 사용자 이메일
    private String phoneNumber; // 휴대폰 번호
    private String address;     // 주소
}
