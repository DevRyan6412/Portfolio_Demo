// src/main/java/com/example/demo/domain/dto/OAuth2UserInfo.java

package com.example.demo.domain.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuth2UserInfo {
    private String email;
    private String name;
    private String provider;
    private String providerId;
}