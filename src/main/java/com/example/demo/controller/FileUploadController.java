package com.example.demo.controller;

import com.example.demo.domain.entity.Issue;
import com.example.demo.domain.entity.IssuePostFile;
import com.example.demo.repository.IssueRepository;
import com.example.demo.service.FileUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;

//import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
public class
FileUploadController {

    private final FileUploadService fileUploadService;
    private final IssueRepository issueRepository;

    public FileUploadController(FileUploadService fileUploadService, IssueRepository issueRepository) {
        this.fileUploadService = fileUploadService;
        this.issueRepository = issueRepository;
    }

    /**
     * 파일 업로드
     */
    @PostMapping("/api/files/upload/{projectId}/{issueId}")
    public ResponseEntity<?> uploadFile(@PathVariable Long projectId,
                                        @PathVariable Long issueId,
                                        @RequestParam("file") MultipartFile file) {
        try {
            // 파일 비어있는지 확인
            if (file.isEmpty()) {
                log.error("파일이 비어있습니다.");
                return ResponseEntity.badRequest().body("파일을 선택해주세요.");
            }
            // 파일 크기 제한 (선택사항)
            long maxFileSize = 1024 * 1024 * 1024; // 1GB
            if (file.getSize() > maxFileSize) {
                log.error("파일 크기 초과: {} bytes", file.getSize());
                return ResponseEntity.badRequest().body("파일 크기는 10MB를 초과할 수 없습니다.");
            }

            // 이슈 존재 여부 확인
            Issue issue = issueRepository.findById(issueId)
                    .orElseThrow(() -> {
                        log.error("이슈를 찾을 수 없습니다. ID: {}", issueId);
                        return new RuntimeException("이슈를 찾을 수 없습니다.");
                    });

            // 파일 저장
            IssuePostFile savedFile = fileUploadService.storeFile(file, issue);
            log.info("파일 업로드 성공: {}", savedFile.getFileName());

            return ResponseEntity.status(HttpStatus.CREATED).body(savedFile);

        } catch (IOException e) {
            log.error("파일 업로드 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("파일 업로드 중 오류가 발생했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("예상치 못한 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 파일 미리보기 (이미지 파일만)
     */
    @GetMapping("/api/files/view/{fileId}")
    public ResponseEntity<Resource> viewFile(@PathVariable Long fileId) {
        try {
            IssuePostFile issuePostFile = fileUploadService.getFileById(fileId);

            if (!issuePostFile.getFileType().startsWith("image/")) {
                throw new RuntimeException("미리보기가 지원되지 않는 파일 형식입니다.");
            }

            // 파일 경로에서 파일명만 추출 (UUID_originalFileName 형식)
            String savedFileName = Paths.get(issuePostFile.getFilePath()).getFileName().toString();

            // 리소스 로드
            Resource resource = fileUploadService.loadFileAsResource(savedFileName).getBody();

            // Content-Type 설정 (이미지 타입에 맞게)
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(issuePostFile.getFileType()))
                    .header("Content-Disposition", "inline; filename=\"" + issuePostFile.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("파일 미리보기 중 오류 발생", e);
            throw new RuntimeException("파일을 미리볼 수 없습니다.", e);
        }
    }

    @GetMapping("/api/files/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        try {
            IssuePostFile issuePostFile = fileUploadService.getFileById(fileId);

            // 파일 경로에서 파일명만 추출 (UUID_originalFileName 형식)
            String savedFileName = Paths.get(issuePostFile.getFilePath()).getFileName().toString();

            // 파일 다운로드
            return fileUploadService.loadFileAsResource(savedFileName, issuePostFile.getFileName());

        } catch (Exception e) {
            log.error("파일 다운로드 중 오류 발생", e);
            throw new RuntimeException("파일을 다운로드할 수 없습니다: " + e.getMessage(), e);
        }
    }

    @PostMapping("/api/files/delete/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable Long fileId) {
        fileUploadService.deleteFileById(fileId);
        return ResponseEntity.ok().body("파일이 삭제되었습니다.");
    }

    /**
     * 특정 이슈의 파일 목록 조회
     */
    @GetMapping("/api/files/list/{issueId}")
    public ResponseEntity<?> getFileList(@PathVariable Long issueId) {
        try {
            return ResponseEntity.ok(fileUploadService.getFilesByIssueId(issueId));
        } catch (Exception e) {
            log.error("파일 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("파일 목록을 불러올 수 없습니다.");
        }
    }

    /**
     * 파일 다운로드 - 이슈 ID와 파일명으로
     */
    @GetMapping(value = {"/api/issues/{issueId}/files/download/{fileName:.+}",
            "api/issues/{issueId}/files/download/{fileName:.+}"},
            produces = MediaType.ALL_VALUE)
    public ResponseEntity<Resource> downloadFileByIssueAndName(
            @PathVariable Long issueId,
            @PathVariable String fileName,
            HttpServletRequest request) {

        String path = request.getServletPath();
        log.info("파일 다운로드 요청: path={}, issueId={}, fileName={}", path, issueId, fileName);

        try {
            // 이슈의 모든 파일 가져오기
            List<IssuePostFile> files = fileUploadService.getFilesByIssueId(issueId);

            // 디버깅: 모든 파일 정보 로깅
            for (IssuePostFile f : files) {
                log.info("파일 정보: ID={}, 원본명={}, 저장경로={}",
                        f.getId(), f.getFileName(), f.getFilePath());
            }

            // 파일명으로 파일 찾기 (원본명 또는 저장된 파일명)
            IssuePostFile file = files.stream()
                    .filter(f -> f.getFileName().equals(fileName) ||
                            Paths.get(f.getFilePath()).getFileName().toString().equals(fileName))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("이슈 {}에서 파일 {}을 찾을 수 없습니다.", issueId, fileName);
                        return new RuntimeException("파일을 찾을 수 없습니다: " + fileName);
                    });

            // 파일 경로에서 저장된 파일명 추출
            String savedFileName = Paths.get(file.getFilePath()).getFileName().toString();

            log.info("파일 찾음: ID={}, 저장된 파일명={}, 원본 파일명={}",
                    file.getId(), savedFileName, file.getFileName());

            // 파일 다운로드
            return fileUploadService.loadFileAsResource(savedFileName, file.getFileName());
        } catch (Exception e) {
            log.error("파일 다운로드 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("파일을 다운로드할 수 없습니다: " + e.getMessage(), e);
        }
    }
    /**
     * 파일 다운로드 처리 공통 메서드
     */
    private ResponseEntity<Resource> processFileDownload(Long issueId, String fileName) {
        try {
            log.info("이슈 ID와 파일명으로 다운로드 처리: issueId={}, fileName={}", issueId, fileName);

            // 이슈의 모든 파일 가져오기
            List<IssuePostFile> files = fileUploadService.getFilesByIssueId(issueId);
            log.info("이슈의 파일 목록 개수: {}", files.size());

            // 파일 정보 로깅
            for (IssuePostFile f : files) {
                log.info("파일: ID={}, 원본명={}, 경로={}", f.getId(), f.getFileName(), f.getFilePath());
            }

            // 파일명이 일치하는 파일 찾기 (원본 파일명으로 비교 또는 경로의 파일명으로 비교)
            IssuePostFile file = files.stream()
                    .filter(f -> f.getFileName().equals(fileName) ||
                            Paths.get(f.getFilePath()).getFileName().toString().equals(fileName))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("이슈 {}에서 파일 {}을 찾을 수 없습니다.", issueId, fileName);
                        return new RuntimeException("파일을 찾을 수 없습니다: " + fileName);
                    });

            // 파일 경로에서 저장된 파일명 추출
            String savedFileName = Paths.get(file.getFilePath()).getFileName().toString();

            log.info("파일 찾음: ID={}, 저장된 파일명={}, 원본 파일명={}",
                    file.getId(), savedFileName, file.getFileName());

            // 파일 다운로드
            return fileUploadService.loadFileAsResource(savedFileName, file.getFileName());
        } catch (Exception e) {
            log.error("파일 다운로드 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("파일을 다운로드할 수 없습니다: " + e.getMessage(), e);
        }
    }
}