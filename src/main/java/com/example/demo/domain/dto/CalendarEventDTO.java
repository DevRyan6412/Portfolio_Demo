package com.example.demo.domain.dto;

import com.example.demo.domain.entity.CalendarEvent;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CalendarEventDTO {
    // 기존 필드 유지
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String color;
    private boolean allDay;

    // Board 상속으로 인한 추가 필드
    private String boardNm;
    private String createdBy;
    private String modifiedBy;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String cName;

    public static CalendarEventDTO fromEntity(CalendarEvent event) {
        CalendarEventDTO dto = new CalendarEventDTO();
        // 기존 매핑
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setStartDate(event.getStartDate());
        dto.setEndDate(event.getEndDate());
        dto.setColor(event.getColor());
        dto.setAllDay(event.isAllDay());

        // Board 필드 매핑 추가
        dto.setBoardNm(event.getBoardNm());
        dto.setCreatedBy(event.getCreatedBy());
        dto.setModifiedBy(event.getModifiedBy());
        dto.setCreatedDate(event.getCreatedDate());
        dto.setModifiedDate(event.getModifiedDate());
        dto.setCName(event.getCName());
        return dto;
    }
}