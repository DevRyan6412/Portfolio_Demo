package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"project", "user"})
public class ProjectMember {

    @EmbeddedId
    private ProjectMemberId id = new ProjectMemberId();

    @ManyToOne(fetch = FetchType.EAGER)//테스트수정(기존 LAZY)
    @MapsId("projectId")
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)//테스트수정(기존 LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_role")
    private ProjectRole projectRole;

    @Version
    private Long version;  // 버전 필드 추가

    // 편의 메소드: 양방향 연관관계 설정
    public void setProject(Project project) {
        this.project = project;
        this.id.setProjectId(project.getId());
    }

    public void setUser(User user) {
        this.user = user;
        // user가 null이 아니라면, id를 설정합니다.
        if (user != null) {
            this.id.setUserId(user.getId());
        }
    }
}
