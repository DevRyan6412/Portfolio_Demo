package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@EntityListeners(value = AuditingEntityListener.class)
@MappedSuperclass // 부모 클래스를 엔티티가 아닌, 공통 필드를 상속하는 클래스로 지정
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Board {//게시판들에 공통적으로 필요한 필드들을 정의하는 추상클래스 board
    private String boardNm; //게시판 이름

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;//작성자 이메일
    @LastModifiedBy
    private String modifiedBy;//수정자 이메일
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate; //등록일
    @LastModifiedDate
    private LocalDateTime modifiedDate; //수정일
    @Column(name = "cname", updatable = false)
    private String cName; //작성자 이름
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
}
