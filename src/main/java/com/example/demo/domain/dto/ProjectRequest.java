package com.example.demo.domain.dto;

import com.example.demo.domain.entity.ProjectStatus;
import com.example.demo.domain.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ProjectRequest {

    @NotBlank(message = "프로젝트 이름은 필수입니다")
    private String projectName;
    @NotBlank(message = "프로젝트 설명은 필수입니다")
    private String description;
    @NotNull(message = "시작일은 필수입니다")
    private String startDate;
    @NotNull(message = "종료 예정일은 필수입니다")
    private String endDate;
    private ProjectStatus status;// index에 상태 뱃지를 위한 status추가
    private User creator;//index에 뱃지를 위한 creator추가
    private List<Long> memberIds;  // 프로젝트에 추가할 멤버들의 ID
}

