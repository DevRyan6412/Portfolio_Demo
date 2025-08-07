package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "calendar_events")
public class CalendarEvent extends Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    private String title;
    private String description;
    @Column(name = "start_date")
    private LocalDateTime startDate;
    @Column(name = "end_date")
    private LocalDateTime endDate;
    private String color;  // 일정 구분을 위한 색상
    private boolean allDay;  // 종일 일정 여부
    @Override
    public String getBoardNm() {
        return "calendar";
    }

    public CalendarEvent(Project project, String cName, String createdBy,
                         String title, String description, LocalDateTime startDate,
                         LocalDateTime endDate, String color, boolean allDay) {
        super();  // Board 클래스의 기본 생성자 호출
        this.setProject(project);  // 프로젝트 설정
        this.setBoardNm(getBoardNm());  // 게시판 이름 설정
        this.setCName(cName);
        this.setCreatedBy(createdBy);
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.color = color;
        this.allDay = allDay;
    }
}