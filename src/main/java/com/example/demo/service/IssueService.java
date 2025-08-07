package com.example.demo.service;

import com.example.demo.domain.dto.IssueDTO;
import com.example.demo.domain.entity.Issue;
import com.example.demo.domain.entity.IssueStatus;
import com.example.demo.domain.entity.Project;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.IssueRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssueService {

    private static final Logger log = LoggerFactory.getLogger(IssueService.class);
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final FileUploadService fileUploadService;
    private final UserRepository userRepository;
    private final LogBoardService logBoardService; // ✅ LogBoardService 의존성 추가

    /**
     * ✅ 특정 프로젝트 내 특정 상태의 이슈 조회
     */

    @Transactional(readOnly = true)
    public Page<IssueDTO> getIssuesByProjectAndStatus(Long projectId, IssueStatus status, Pageable pageable) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("해당 프로젝트를 찾을 수 없습니다."));

        return issueRepository.findByProjectAndStatus(project, status, pageable)
                .map(IssueDTO::fromEntity);
    }


//    @Transactional(readOnly = true)
//    public List<IssueDTO> getIssuesByProjectAndStatus(Long projectId, IssueStatus status) {
//        Project project = projectRepository.findById(projectId)
//                .orElseThrow(() -> new RuntimeException("해당 프로젝트를 찾을 수 없습니다."));
//
//        return issueRepository.findByProjectAndStatus(project, status).stream()
//                .map(IssueDTO::fromEntity)
//                .collect(Collectors.toList());
//    }

    /**
     * ✅ 특정 프로젝트의 모든 이슈 조회
     */
    @Transactional(readOnly = true)
    public List<IssueDTO> getAllIssuesByProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("해당 프로젝트를 찾을 수 없습니다."));

        return issueRepository.findByProject(project).stream()
                .map(IssueDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // ✅ 새로운 방식 (페이징 적용)
    @Transactional(readOnly = true)
    public Page<IssueDTO> getAllIssuesByProjectSorted(Long projectId, String sortBy, Pageable pageable) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("해당 프로젝트를 찾을 수 없습니다."));

        Sort sort = getSortOrder(sortBy);
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        return issueRepository.findByProject(project, sortedPageable)
                .map(issue -> {
                    IssueDTO dto = IssueDTO.fromEntity(issue);
                    if (dto.getStatus() == null) {
                        dto.setStatus(IssueStatus.IN_PROGRESS); // ✅ 기본값 적용
                    }
                    return dto;
                });
    }


    private Sort getSortOrder(String sortBy) {
        return switch (sortBy) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdDate");
            case "author" -> Sort.by(Sort.Direction.ASC, "cName");
            default -> Sort.by(Sort.Direction.DESC, "createdDate");
        };
    }




    /**
     * ✅ 특정 프로젝트의 이슈 검색
     */

    @Transactional(readOnly = true)
    public Page<IssueDTO> getIssuesByProjectAndKeywordSorted(Long projectId, String keyword, String sortBy, Pageable pageable) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("해당 프로젝트를 찾을 수 없습니다."));

        Sort sort = getSortOrder(sortBy);
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        return issueRepository.searchIssues(project, keyword, sortedPageable)
                .map(IssueDTO::fromEntity);
    }

//    private Sort getSortOrder(String sortBy) {
//        return switch (sortBy) {
//            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdDate"); // 오래된순
//            case "author" -> Sort.by(Sort.Direction.ASC, "cName"); // 작성자순
//            default -> Sort.by(Sort.Direction.DESC, "createdDate"); // 최신순 (기본값)
//        };
//    }


    /**
     * ✅ 특정 프로젝트 내 특정 이슈 조회
     */
    @Transactional(readOnly = true)
    public IssueDTO getIssueByIdAndProject(Long id, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("해당 프로젝트를 찾을 수 없습니다."));
        Issue issue = issueRepository.findByIdAndProject(id, project)
                .orElseThrow(() -> new RuntimeException("해당 프로젝트에서 이슈를 찾을 수 없습니다."));
        return IssueDTO.fromEntity(issue);
    }

    /**
     * ✅ 특정 프로젝트에 이슈 생성 (파일 없는 경우)
     */
    @Transactional
    public void createIssue(IssueDTO issueDTO, Long projectId) {
        createIssue(issueDTO, projectId, null);  // ✅ 파일이 없는 경우에도 실행 가능하도록 오버로딩
    }

    /**
     * ✅ 특정 프로젝트에 이슈 생성 (파일 포함)
     */
    @Transactional
    public void createIssue(IssueDTO issueDTO, Long projectId, MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            // 사용자 정보 처리
            String userEmail = null;
            if (authentication.getPrincipal() instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                // OAuth2 속성에서 이메일 추출
                userEmail = oauth2User.getAttribute("email");
            } else if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
                userEmail = ((org.springframework.security.core.userdetails.User) authentication.getPrincipal()).getUsername();
            } else {
                userEmail = authentication.getName();
            }

            // 이메일로 사용자 조회 후 실제 사용자 이름 획득
            if (userEmail != null) {
                Optional<User> userOptional = userRepository.findByEmail(userEmail);
                if (userOptional.isPresent()) {
                    issueDTO.setCName(userOptional.get().getName());
                } else {
                    // 이메일로 사용자를 찾을 수 없는 경우 기본값 설정
                    issueDTO.setCName("Unknown User");
                }
            }
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("해당 프로젝트를 찾을 수 없습니다."));

        Issue issue = new Issue(issueDTO, project);
        issue.setStatus(IssueStatus.IN_PROGRESS);

        Issue savedIssue = issueRepository.save(issue);

        if (file != null && !file.isEmpty()) {
            try {
                fileUploadService.storeFile(savedIssue.getId(), file);
            } catch (IOException e) {
                throw new RuntimeException("파일 저장 중 오류 발생", e);
            }
        }

        // ✅ 이슈 생성 로그 추가
        logBoardService.saveLog(
                savedIssue.getProject(),
                "issues",  // 게시판 이름, 이슈 서비스에서는 "Issue"로 통일
                savedIssue.getId(),
                savedIssue.getCreatedBy(),
                savedIssue.getModifiedBy(),
                "ADD",
                LocalDateTime.now(),
                savedIssue.getCName()
        );
    }

    /**
     * ✅ 특정 프로젝트 내 특정 이슈 수정
     */

    @Transactional
    public IssueDTO updateIssue(Long id, IssueDTO issueDTO, Long projectId, MultipartFile[] newFiles) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("해당 프로젝트를 찾을 수 없습니다."));

        Issue issue = issueRepository.findByIdAndProject(id, project)
                .orElseThrow(() -> new RuntimeException("해당 프로젝트에서 이슈를 찾을 수 없습니다."));

        issue.setTitle(issueDTO.getTitle());
        issue.setContent(issueDTO.getContent());
        issue.setStatus(issueDTO.getStatus() != null ? issueDTO.getStatus() : IssueStatus.IN_PROGRESS); // ✅ 상태 값 보장
        issue.setModifiedBy(issueDTO.getModifiedBy());

        Issue updatedIssue = issueRepository.save(issue);



        // ✅ 새 파일 저장 로직 추가
        if (newFiles != null) {
            for (MultipartFile file : newFiles) {
                if (!file.isEmpty()) {
                    try {
                        fileUploadService.storeFile(updatedIssue.getId(), file);
                    } catch (IOException e) {
                        throw new RuntimeException("파일 저장 중 오류 발생", e);
                    }
                }
            }
        }



        // ✅ 이슈 수정 로그 추가
        logBoardService.saveLog(
                updatedIssue.getProject(),
                "issues",  // 게시판 이름, 이슈 서비스에서는 "Issue"로 통일
                updatedIssue.getId(),
                updatedIssue.getCreatedBy(),
                updatedIssue.getModifiedBy(),
                "Update",
                LocalDateTime.now(),
                updatedIssue.getCName()
        );

        return IssueDTO.fromEntity(updatedIssue);
    }

    /**
     * ✅ 특정 프로젝트 내 특정 이슈 삭제
     */
    @Transactional
    public void deleteIssue(Long id, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("해당 프로젝트를 찾을 수 없습니다."));

        Issue issue = issueRepository.findByIdAndProject(id, project)
                .orElseThrow(() -> new RuntimeException("해당 프로젝트에서 이슈를 찾을 수 없습니다."));

        // ✅ 이슈 삭제 로그 추가 (삭제 전에 로그를 남겨야 함)
        logBoardService.saveLog(
                issue.getProject(),
                "issues",  // 게시판 이름, 이슈 서비스에서는 "Issue"로 통일
                issue.getId(),
                issue.getCreatedBy(),
                issue.getModifiedBy(),
                "DELETE",
                LocalDateTime.now(),
                issue.getCName()
        );

        issueRepository.delete(issue);
    }
}