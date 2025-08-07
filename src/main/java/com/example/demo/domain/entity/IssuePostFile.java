package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "issue_post_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssuePostFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;  // ✅ 이슈와 연결

    private String fileName;   // ✅ 원본 파일명
    private String filePath;   // ✅ 저장된 파일 경로
    private String fileType;   // ✅ 파일 유형
    private long fileSize;     // ✅ 파일 크기 (Byte)
}

//✅ 파일을 DB에서 조회할 수 있도록 IssuePostFile 엔티티 추가