package com.example.demo.service;

import com.example.demo.domain.dto.NoticeBoardDTO;
import com.example.demo.domain.entity.NoticeBoard;
import com.example.demo.domain.entity.Project;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.NoticeBoardRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeBoardService {

    private static final Logger log = LoggerFactory.getLogger(NoticeBoardService.class);
    private final NoticeBoardRepository noticeBoardRepository;
    private final ProjectRepository projectRepository;
    private final LogBoardService logBoardService;
    private final UserRepository userRepository;


    //게시글 등록11111111111111111
    //프로젝트 ID에 해당하는 프로젝트를 먼저 조회하고, 게시글 엔티티에 연결한 후 저장합니다.
    //현재 인증된 사용자 이름을 cName 필드에 설정.
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public NoticeBoard saveNoticeBoard(NoticeBoard noticeBoard, Long projectId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail;
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            userEmail = oauth2User.getAttribute("email");
        } else {
            userEmail = authentication.getName();
        }

        // 이메일로 사용자 조회 후 실제 사용자 이름을 획득
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userEmail));
        // 실제 사용자 이름을 cName에 설정
        noticeBoard.setCName(user.getName());

        // 프로젝트 조회 및 게시글 연결
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. id: " + projectId));
        noticeBoard.setProject(project);

        NoticeBoard savedBoard = noticeBoardRepository.save(noticeBoard);

        // 로그 기록: 수정된 사용자 정보가 저장됨
        logBoardService.saveLog(
                savedBoard.getProject(),
                savedBoard.getBoardNm(),
                savedBoard.getId(),
                savedBoard.getCreatedBy(),
                savedBoard.getModifiedBy(),
                "ADD",
                LocalDateTime.now(),
                savedBoard.getCName()
        );

        return savedBoard;
    }



    //게시글 상세 조회
    //프로젝트 ID와 게시글 ID를 기반으로 해당 게시글이 올바른 프로젝트에 속하는지 확인한 후 DTO로 변환하여 반환.
    public NoticeBoardDTO getNoticeBoardByIdAndProject(Long id, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. id: " + projectId));
        NoticeBoard noticeBoard = noticeBoardRepository.findByIdAndProject(id, project)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다. id: " + id));
        return NoticeBoardDTO.fromEntity(noticeBoard);
    }

    //전체 게시글 조회
    //특정 프로젝트에 속한 모든 게시글을 조회하여 DTO 목록으로 반환합니다.
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public List<NoticeBoardDTO> getAllNoticeBoardsByProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. id: " + projectId));
        List<NoticeBoard> noticeBoards = noticeBoardRepository.findByProject(project);
        return noticeBoards.stream()
                .map(NoticeBoardDTO::fromEntity)
                .collect(Collectors.toList());
    }
    //NoticeBoard 페이징
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public Page<NoticeBoardDTO> getNoticeBoardsByProjectPaging(Long projectId, Pageable pageable) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. id: " + projectId));
        Page<NoticeBoard> noticeBoardPage = noticeBoardRepository.findByProject(project, pageable);
        return noticeBoardPage.map(NoticeBoardDTO::fromEntity);
    }

    //게시글 수정
    //프로젝트 ID와 게시글 ID를 기반으로 해당 게시글을 조회한 후, DTO의 내용으로 업데이트합니다.
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public NoticeBoard updateNoticeBoard(Long id, NoticeBoardDTO noticeBoardDTO, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. id: " + projectId));
        NoticeBoard noticeBoard = noticeBoardRepository.findByIdAndProject(id, project)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id: " + id));
        noticeBoard.setTitle(noticeBoardDTO.getTitle());
        noticeBoard.setModifiedBy(noticeBoardDTO.getModifiedBy());
        noticeBoard.setContents(noticeBoardDTO.getContents());

        NoticeBoard updatedBoard = noticeBoardRepository.save(noticeBoard);
        logBoardService.saveLog(
                updatedBoard.getProject(),
                updatedBoard.getBoardNm(),
                updatedBoard.getId(),
                updatedBoard.getCreatedBy(),
                updatedBoard.getModifiedBy(),
                "Update",
                LocalDateTime.now(),
                updatedBoard.getCName()
        );
        return updatedBoard;
    }

    //게시글 삭제
    //프로젝트 ID와 게시글 ID를 기반으로 해당 게시글이 올바른 프로젝트에 속하는지 확인한 후 삭제.
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public void deleteNoticeBoard(Long id, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. id: " + projectId));
        NoticeBoard noticeBoard = noticeBoardRepository.findByIdAndProject(id, project)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id: " + id));
        noticeBoardRepository.delete(noticeBoard);
        // 필요 시 삭제 로그 추가 가능
    }
}
