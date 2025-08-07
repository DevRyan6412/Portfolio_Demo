package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String projectName;

    private String description;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProjectMember> projectMembers = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoticeBoard> noticeBoards = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LogBoard> logBoards = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CalendarEvent> calendarEvents = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Issue> issue = new ArrayList<>();


    @Version
    private Long version;  // 버전 필드 추가

    public void createDefaultBoards(NoticeBoard defaultNotice, LogBoard defaultLog) {
        this.noticeBoards.add(defaultNotice);
        this.logBoards.add(defaultLog);
    }

    public void addProjectMember(ProjectMember projectMember) {
        projectMembers.add(projectMember);
        projectMember.setProject(this);
    }

    public void removeProjectMember(ProjectMember projectMember) {
        projectMembers.remove(projectMember);
    }

    @Override
    public String toString() {
        return "Project{id=" + id + ", projectName='" + projectName + "', creator=" + (user != null ? user.getId() : "null") + "}";
    }
}