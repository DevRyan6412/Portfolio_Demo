package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "global_notice")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gId;

    @Column(nullable = false)
    private String gTitle;  // 기존 gTitle 대신 title

    @Column(length = 5000, nullable = false)
    private String gContents;  // 기존 gContents 대신 contents

    // Board와 유사한 공통 필드들
    private String boardNm;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedBy
    private String modifiedBy;

    @LastModifiedDate
    private LocalDateTime modifiedDate;

    @Column(name = "cname", updatable = false)
    private String cName;

    @PrePersist
    public void prePersist() {
        this.cName = "ADMIN";
        this.boardNm = "globalnotice";
    }
}
