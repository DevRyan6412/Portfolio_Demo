package com.example.demo.service;

import com.example.demo.repository.IssueRepository;
import com.example.demo.domain.entity.Issue;
import com.example.demo.domain.entity.IssuePostFile;
import com.example.demo.repository.IssuePostFileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final IssuePostFileRepository issuePostFileRepository;
    private final IssueRepository issueRepository;

    @Value("${upload.path:/var/app/current/uploads}")
    private String uploadDir;

    /**
     * 파일 저장 메서드 (이슈 엔티티와 함께)
     */
    public IssuePostFile storeFile(MultipartFile file, Issue issue) throws IOException {
        // 로깅 추가
        log.info("파일 업로드 시작 - 원본 파일명: {}", file.getOriginalFilename());
        log.info("업로드 디렉토리: {}", uploadDir);

        // 원본 파일 이름 정리
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        // 고유한 파일 이름 생성 (UUID 사용)
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;

        // 업로드 디렉토리 경로 생성
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

// ✅ 디렉토리가 없으면 생성하도록 수정 (기존 코드 유지)
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
                log.info("📂 업로드 디렉토리 생성 완료: {}", uploadPath.toAbsolutePath());
            } catch (IOException e) {
                log.error("❌ 업로드 디렉토리 생성 실패", e);
                throw new IOException("업로드 디렉토리를 생성할 수 없습니다.", e);
            }
        }


        // 파일 저장 경로
        Path targetLocation = uploadPath.resolve(uniqueFileName);

        try {
            // 파일 저장
            Files.copy(file.getInputStream(), targetLocation);
            log.info("파일 저장 완료: {}", targetLocation);
        } catch (IOException e) {
            log.error("파일 저장 실패", e);
            throw new IOException("파일을 저장할 수 없습니다.", e);
        }

        // DB에 파일 정보 저장
        IssuePostFile issuePostFile = IssuePostFile.builder()
                .issue(issue)
                .fileName(originalFileName)
                .filePath(targetLocation.toString())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .build();

        try {
            return issuePostFileRepository.save(issuePostFile);
        } catch (Exception e) {
            log.error("파일 정보 DB 저장 실패", e);
            throw new RuntimeException("파일 정보를 저장할 수 없습니다.", e);
        }
    }

    /**
     * 이슈 ID로 파일 저장 메서드 오버로딩
     */
    public IssuePostFile storeFile(Long issueId, MultipartFile file) throws IOException {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> {
                    log.error("이슈를 찾을 수 없습니다. ID: {}", issueId);
                    return new RuntimeException("이슈를 찾을 수 없습니다.");
                });

        return storeFile(file, issue);
    }

    /**
     * 특정 이슈의 파일 목록 가져오기
     */
    public List<IssuePostFile> getFilesByIssueId(Long issueId) {
        return issuePostFileRepository.findByIssueId(issueId);
    }

    /**
     * ID로 파일 정보 조회
     */
    public IssuePostFile getFileById(Long fileId) {
        return issuePostFileRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error("파일을 찾을 수 없습니다. ID: {}", fileId);
                    return new RuntimeException("파일을 찾을 수 없습니다.");
                });
    }

    /**
     * 파일을 리소스로 로드 - 파일명으로만 로드 (기존 메서드)
     */

    public ResponseEntity<Resource> loadFileAsResource(String fileName) {
        try {
            // UUID_originalFileName 형식으로 저장된 파일명 처리
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            log.info("파일 로드 시도: {}", filePath.toString());

            Resource resource = new UrlResource(filePath.toAbsolutePath().toUri());

            if (resource.exists() && resource.isReadable()) {
                // 올바른 Content-Type 설정

                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }


                log.info("파일 타입: {}", contentType);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);

            } else {
                log.error("❌ 파일이 존재하지 않음: " + filePath.toAbsolutePath());
                throw new RuntimeException("파일을 찾을 수 없습니다: " + fileName);
            }
        } catch (Exception e) {
            log.error("❌ 파일 다운로드 중 오류 발생", e);
            throw new RuntimeException("파일을 찾을 수 없습니다: " + fileName, e);
        }
    }
    /**
     * 파일 삭제 기능 추가
     */
    public void deleteFileById(Long fileId) {
        IssuePostFile issuePostFile = issuePostFileRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error("파일을 찾을 수 없습니다. ID: {}", fileId);
                    return new RuntimeException("파일을 찾을 수 없습니다.");
                });

        try {
            Path filePath = Paths.get(issuePostFile.getFilePath()).normalize();
            Files.deleteIfExists(filePath);
            log.info("🗑️ 파일 삭제 완료: {}", filePath);
        } catch (IOException e) {
            log.error("❌ 파일 삭제 실패: {}", issuePostFile.getFilePath(), e);
        }

        issuePostFileRepository.delete(issuePostFile);
        log.info("🗑️ DB에서 파일 정보 삭제 완료: {}", fileId);
    }

    /**
     * 파일을 리소스로 로드 - 저장된 파일명과 원본 파일명 구분 (새로운 메서드)
     */
    public ResponseEntity<Resource> loadFileAsResource(String savedFileName, String originalFileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(savedFileName).normalize();
            log.info("파일 로드 시도: {}, 원본 파일명: {}", filePath.toString(), originalFileName);

            Resource resource = new UrlResource(filePath.toAbsolutePath().toUri());

            if (resource.exists() && resource.isReadable()) {
                // 올바른 Content-Type 설정
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";  // 기본 값
                }

                log.info("파일 타입: {}", contentType);

                // 한글 파일명 처리
                String encodedFileName = new String(originalFileName.getBytes("UTF-8"), "ISO-8859-1");

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                        .body(resource);
            } else {
                log.error("파일을 찾을 수 없거나 읽을 수 없습니다: {}", savedFileName);
                throw new RuntimeException("파일을 찾을 수 없거나 읽을 수 없습니다: " + savedFileName);
            }
        } catch (IOException e) {
            log.error("파일을 로드할 수 없습니다", e);
            throw new RuntimeException("파일을 로드할 수 없습니다: " + savedFileName, e);
        }
    }
}

