package com.example.demo.domain.entity;

import com.example.demo.domain.dto.IssueDTO;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Issue extends Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Override
    public String getBoardNm() {
        return "issues";
    }

    // 파일 리스트 (첨부 파일 등)
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IssuePostFile> files = new ArrayList<>();

    // 댓글 목록 (cascade 시 댓글과 답글이 함께 삭제됨)
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IssueComment> comments = new ArrayList<>();

    // ✅ 추가: 이슈 상태 필드
    @Enumerated(EnumType.STRING)
    @Column(nullable = true, columnDefinition = "VARCHAR(20) DEFAULT 'IN_PROGRESS'")
    private IssueStatus status = IssueStatus.IN_PROGRESS;

    // 기존 생성자 유지
    public Issue(Project project, String cName, String createdBy, String title, String content) {
        super();
        this.setProject(project);
        this.setBoardNm(getBoardNm());
        this.setCName(cName);
        this.setCreatedBy(createdBy);
        this.title = title;
        this.content = content;
        this.status = IssueStatus.IN_PROGRESS; // 기본값 설정
    }

    // IssueDTO와 Project를 이용한 생성자
    public Issue(IssueDTO issueDTO, Project project) {
        super();
        this.setProject(project);
        this.setBoardNm(getBoardNm());
        this.setCName(issueDTO.getCName());
        this.setCreatedBy(issueDTO.getCreatedBy());
        this.title = issueDTO.getTitle();
        this.content = issueDTO.getContent();
        this.status = IssueStatus.IN_PROGRESS; // 기본값 설정
    }
}

//기본값을 "진행 중(IN_PROGRESS)"으로 설정