package com.example.demo.domain.dto;

import com.example.demo.domain.entity.NoticeBoard;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeBoardDTO {
    private Long id;
    private String title;
    private String contents;
    private String boardNm;
    private String createdBy;
    private String modifiedBy;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String cName;

    public static NoticeBoardDTO fromEntity(NoticeBoard noticeBoard) {
        return new NoticeBoardDTO(
                noticeBoard.getId(),
                noticeBoard.getTitle(),
                noticeBoard.getContents(),
                noticeBoard.getBoardNm(),
                noticeBoard.getCreatedBy(),
                noticeBoard.getModifiedBy(),
                noticeBoard.getCreatedDate(),
                noticeBoard.getModifiedDate(),
                noticeBoard.getCName()
        );
    }

    public String getBoardNm() {
        return "noticeboard";// 게시판 이름을 고정 값으로 반환
    }
}