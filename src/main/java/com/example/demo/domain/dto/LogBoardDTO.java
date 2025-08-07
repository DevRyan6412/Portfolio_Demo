package com.example.demo.domain.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogBoardDTO {
    private Long logNo;
    private String boardNm;
    private Long postId;
    private String action;
    private String createdBy;
    private String modifiedBy;
    private LocalDateTime actionDate;
    private String cName;
}

