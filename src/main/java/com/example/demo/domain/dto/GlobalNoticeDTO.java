package com.example.demo.domain.dto;

import com.example.demo.domain.entity.GlobalNotice;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GlobalNoticeDTO {
    private Long gId;
    private String gTitle;
    private String gContents;

    // Board와 유사한 공통 필드들
    private String boardNm;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
    private String cName;

    public static GlobalNoticeDTO from(GlobalNotice notice) {
        GlobalNoticeDTO dto = new GlobalNoticeDTO();
        dto.setGId(notice.getGId());
        dto.setGTitle(notice.getGTitle());
        dto.setGContents(notice.getGContents());
        dto.setBoardNm("globalnotice");
        dto.setCreatedBy("admin@admin.com");
        dto.setCreatedDate(notice.getCreatedDate());
        dto.setModifiedBy(notice.getModifiedBy());
        dto.setModifiedDate(notice.getModifiedDate());
        dto.setCName("ADMIN");
        return dto;
    }
}