package com.example.demo.domain.dto;

import com.example.demo.domain.entity.IssueComment;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueCommentDTO {

    private Long id;
    private Long issueId;
    private String contents;
    private String boardNm;
    private String createdBy;
    private String modifiedBy;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String cName;

    // 추가: 부모 댓글 ID
    private Long parentCommentId;

    public static IssueCommentDTO fromEntity(IssueComment comments) {
        return IssueCommentDTO.builder()
                .cName(comments.getUser().getName())
                .id(comments.getId())
                .issueId(comments.getIssue().getId())
                .contents(comments.getContents())
                .createdBy(comments.getUser().getEmail()) // 작성자 이메일
                .createdDate(comments.getCreatedDate())
                .modifiedDate(comments.getModifiedDate())
                // 추가: 부모 댓글 ID 매핑
                .parentCommentId(comments.getParentComment() != null ? comments.getParentComment().getId() : null)
                .build();
    }
}