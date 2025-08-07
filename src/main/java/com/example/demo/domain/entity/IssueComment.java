package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "issue_comment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueComment extends Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 댓글이 달린 이슈
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    // 댓글 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 댓글 내용
    @Column(nullable = false, length = 1000)
    private String contents;

    // 부모 댓글 (없으면 최상위 댓글)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private IssueComment parentComment;

    // 자식 댓글(답글) 목록 – cascade 옵션에 의해 부모 삭제 시 함께 삭제됨
    @Builder.Default
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IssueComment> replies = new ArrayList<>();

    @Override
    public String getBoardNm() {
        return "comment";
    }

    // 생성자 예시 (필요 시 추가)
    public IssueComment(Project project, String cName, String createdBy, String contents, IssueComment parentComment) {
        super();
        this.setProject(project);
        this.setBoardNm(getBoardNm());
        this.setCName(cName);
        this.setCreatedBy(createdBy);
        this.contents = contents;
        this.parentComment = parentComment;
    }
}
