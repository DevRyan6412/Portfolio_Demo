package com.example.demo.domain.dto;

import com.example.demo.domain.entity.Issue;
import com.example.demo.domain.entity.IssuePostFile;
import com.example.demo.domain.entity.IssueStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueDTO {

    private Long id;
    private String title;
    private String content;
    private String boardNm; // 게시판 이름
    private String createdBy; // 작성자 이메일
    private String modifiedBy; // 수정자 이메일
    private LocalDateTime createdDate; // 등록일
    private LocalDateTime modifiedDate; // 수정일
    private String cName; // 작성자 이름
    private List<IssuePostFile> files; // 첨부 파일 목록
    private IssueStatus status; // ✅ 이슈 상태 추가
    private Long projectId; // ✅ 프로젝트 ID 추가

    public static IssueDTO fromEntity(Issue issue) {
        return IssueDTO.builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .content(issue.getContent())
                .boardNm(issue.getBoardNm())
                .createdBy(issue.getCreatedBy())
                .modifiedBy(issue.getModifiedBy())
                .createdDate(issue.getCreatedDate())
                .modifiedDate(issue.getModifiedDate())
                .cName(issue.getCName())
                .files(issue.getFiles()) // ✅ Issue 엔티티에서 파일 리스트 가져오기
                .status(issue.getStatus()) // ✅ Issue 엔티티에서 상태 가져오기
                .projectId(issue.getProject().getId()) // ✅ 프로젝트 ID 변환 추가
                .build();
    }

    public String getBoardNm() {
        return "issues";// 게시판 이름을 고정 값으로 반환

    }
}