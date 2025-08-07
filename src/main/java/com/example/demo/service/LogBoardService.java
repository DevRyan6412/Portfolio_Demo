package com.example.demo.service;

import com.example.demo.domain.entity.LogBoard;
import com.example.demo.domain.entity.Project;
import com.example.demo.repository.LogBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LogBoardService {
    private final LogBoardRepository logBoardRepository;

    public void saveLog(Project project, String boardNm, Long postId, String createdBy, String modifiedBy, String action, LocalDateTime actionDate, String cName) {
        LogBoard log = LogBoard.builder()
                .boardNm(boardNm)
                .postId(postId)
                .action(action)
                .actionDate(actionDate)
                .build();
        log.setCreatedBy(createdBy);//작성자 직접설정
        log.setModifiedBy(modifiedBy);//작성자 직접설정
        log.setCName(cName);
        log.setProject(project);
        logBoardRepository.save(log);
    }

    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public Page<LogBoard> getLogsByProject(Long projectId, Pageable pageable) {
        // 예: repository에서 project_id로 필터링해서 페이징 조회
        return logBoardRepository.findByProjectIdOrderByActionDateDesc(projectId, pageable);
    }

    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public List<LogBoard> getLatestLogs(Long projectId) {
        return logBoardRepository.findByProjectIdOrderByActionDateDesc(
                projectId,
                PageRequest.of(0, 3, Sort.by("actionDate").descending())
        ).getContent();
    }

    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public List<LogBoard> getIndexLogs(Long projectId) {
        return logBoardRepository.findByProjectIdOrderByActionDateDesc(
                projectId,
                PageRequest.of(0, 1, Sort.by("actionDate").descending())
        ).getContent();
    }

}
