package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
//@Inheritance(strategy = InheritanceType.JOINED) // 상속 전략 설정
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NoticeBoard extends Board {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 5000)//VARCHAR 255 => 5000 글자수 늘림
    private String contents;

    @Override//각 게시판 Entity별로 추가할 사항
    public String getBoardNm() {
        return "noticeboard";// 게시판 이름을 고정 값으로 반환
    }

    public NoticeBoard(Project project, String cName, String createdBy, String title, String contents) {
        super();  // Board 클래스의 기본 생성자 호출
        this.setProject(project);  // 프로젝트 설정
        this.setBoardNm(getBoardNm());  // 게시판 이름 설정
        this.setCName(cName);
        this.setCreatedBy(createdBy);
        this.title = title;
        this.contents = contents;
    }
}
