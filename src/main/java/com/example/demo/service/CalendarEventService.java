package com.example.demo.service;

import com.example.demo.domain.dto.CalendarEventDTO;
import com.example.demo.domain.entity.CalendarEvent;
import com.example.demo.domain.entity.Project;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.CalendarEventRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarEventService {
    private final CalendarEventRepository calendarEventRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final LogBoardService logBoardService;

    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public List<CalendarEventDTO> getEventsByDateRange(Long projectId, LocalDateTime start, LocalDateTime end) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        return calendarEventRepository.findByProjectAndStartDateBetween(
                        project,
                        start.minusYears(10),  // 10년 전부터
                        end.plusYears(10)      // 10년 후까지
                )
                .stream()
                .map(CalendarEventDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public CalendarEventDTO createEvent(Long projectId, CalendarEventDTO eventDTO) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        // OAuth2 사용자 정보 가져오기
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail;

        if (auth.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) auth.getPrincipal();
            userEmail = oauth2User.getAttribute("email");
        } else {
            userEmail = auth.getName();
        }

        // 이메일로 사용자 찾기
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CalendarEvent event = new CalendarEvent(
                project,
                user.getName(),  // 실제 사용자 이름
                userEmail,       // 이메일
                eventDTO.getTitle(),
                eventDTO.getDescription(),
                eventDTO.getStartDate().plusHours(9),
                eventDTO.getEndDate().plusHours(9),
                eventDTO.getColor(),
                eventDTO.isAllDay()
        );

        CalendarEvent savedEvent = calendarEventRepository.save(event);

        logBoardService.saveLog(
                savedEvent.getProject(),
                savedEvent.getBoardNm(),
                savedEvent.getId(),
                savedEvent.getCreatedBy(),
                savedEvent.getModifiedBy(),
                "ADD",
                LocalDateTime.now(),
                savedEvent.getCName()
        );

        return CalendarEventDTO.fromEntity(savedEvent);
    }

    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public CalendarEventDTO updateEvent(Long projectId, Long eventId, CalendarEventDTO eventDTO) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        CalendarEvent event = calendarEventRepository.findByIdAndProject(eventId, project)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        event.setTitle(eventDTO.getTitle());
        event.setDescription(eventDTO.getDescription());
        event.setStartDate(eventDTO.getStartDate().plusHours(9));
        event.setEndDate(eventDTO.getEndDate().plusHours(9));
        event.setColor(eventDTO.getColor());
        event.setAllDay(eventDTO.isAllDay());

        CalendarEvent updatedEvent = calendarEventRepository.save(event);

        logBoardService.saveLog(
                updatedEvent.getProject(),
                updatedEvent.getBoardNm(),
                updatedEvent.getId(),
                updatedEvent.getCreatedBy(),
                updatedEvent.getModifiedBy(),
                "Update",
                LocalDateTime.now(),
                updatedEvent.getCName()
        );

        return CalendarEventDTO.fromEntity(updatedEvent);
    }

    @PreAuthorize("@projectSecurity.hasAccessToProject(#projectId, principal)")
    public void deleteEvent(Long projectId, Long eventId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        CalendarEvent event = calendarEventRepository.findByIdAndProject(eventId, project)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        calendarEventRepository.delete(event);
    }
}