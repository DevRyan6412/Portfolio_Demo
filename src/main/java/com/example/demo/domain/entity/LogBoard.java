package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_board")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class LogBoard extends Board{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logNo; //Log번호

    private String boardNm; // 어떤 게시판에서 발생한 로그인지

    // 게시글 ID값을 저장하는 필드 (게시물마다 고유한 ID)
    private Long postId;

    private String action; //변동사항(등록, 수정, 삭제)

    private LocalDateTime actionDate; //등록일

    // User와의 연관관계 추가 (예: 작성자 정보를 저장) //안됨;
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id")
//    private User user;

    @Builder
    public LogBoard(Project project, String boardNm, Long postId,String cName, String action, LocalDateTime actionDate) {
        this.boardNm = boardNm;
        this.postId = postId;
        this.setCName(cName);
        this.action = action;
        this.actionDate = actionDate;
        this.setProject(project);//
    }
}
