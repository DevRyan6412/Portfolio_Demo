package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Invitation extends Board{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 초대 대상 이메일
    @Column(nullable = false)
    private String email;
    // 초대된 프로젝트와의 연관관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    // 초대 토큰 (URL에 포함될 값, UUID를 사용하여 생성)
    @Column(nullable = false, unique = true)
    private String token = UUID.randomUUID().toString();
    // 초대 상태 (예: PENDING, ACCEPTED, EXPIRED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;
    // 생성일
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    // 만료일 (3일 후 만료)
    @Column(nullable = false)
    private LocalDateTime expiresAt = LocalDateTime.now().plusDays(3);

    @Override//각 게시판 Entity별로 추가할 사항
    public String getBoardNm() {
        return "invitation";// 게시판 이름을 고정 값으로 반환
    }

    public Invitation(Project project, String cName, String createdBy, String email) {
        super();  // Board 클래스의 기본 생성자 호출
        this.setProject(project);  // 프로젝트 설정
        this.setBoardNm(getBoardNm());  // 게시판 이름 설정
        this.setCName(cName);
        this.setCreatedBy(createdBy);
        this.email = email;
    }
}