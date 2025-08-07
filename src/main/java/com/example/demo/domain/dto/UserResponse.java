package com.example.demo.domain.dto;

import com.example.demo.domain.entity.Role;
import com.example.demo.domain.entity.User;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String address;
    private Role role;
    private LocalDateTime createdAt;
    private String phoneNumber;
    private String provider;


    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .address(user.getAddress())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .phoneNumber(user.getPhoneNumber())
                .provider(user.getProvider())

                .build();
    }
}