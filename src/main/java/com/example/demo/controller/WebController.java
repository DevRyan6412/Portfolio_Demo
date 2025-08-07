package com.example.demo.controller;

import com.example.demo.domain.entity.Project;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WebController {

    @Autowired
    private UserRepository userRepository;

    private final ProjectService projectService;

    public WebController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/projects/{projectId}/chat")
    public String projectChatPage(@PathVariable String projectId, Model model) {
        // 프로젝트별 채팅 페이지 
        model.addAttribute("projectId", projectId);
        //RYAN navbar에서 project.projectName 을 쓰기위해 모델 추가----여기부터
        // String 타입의 projectId를 Long으로 변환
        Long id = Long.parseLong(projectId);
        // 변환한 id를 이용해 프로젝트 객체를 조회
        Project project = projectService.getProjectById(id);
        // 모델에 프로젝트 id와 객체 추가 (예: navbar에서 project.projectName 사용)
        model.addAttribute("projectId", id);
        model.addAttribute("project", project);//RYAN navbar에서 project.projectName 을 쓰기위해 모델 추가----여기까지
        return "chat";
    }
}