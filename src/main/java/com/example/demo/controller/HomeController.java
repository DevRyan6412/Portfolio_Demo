package com.example.demo.controller;

import com.example.demo.domain.entity.LogBoard;
import com.example.demo.domain.entity.Project;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.LogBoardService;
import com.example.demo.service.ProjectService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectService projectService;

    @GetMapping("/")
    public String home(Authentication authentication, @RequestParam(required = false) Long id, Model model) {
        String username = "Unknown User";
        String email = null;

        List<Project> projects = new ArrayList<>();

        if (authentication != null) {
            if (authentication.getPrincipal() instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                if (oauth2User.getAttribute("email") != null) {
                    email = (String) oauth2User.getAttribute("email");
//                    log.info("OAuth2 로그인 사용자 이메일: {}", email);
                }
            } else if (authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                email = userDetails.getUsername();
//                log.info("일반 로그인 사용자 이메일: {}", email);
            }

            if (email != null) {
                User user = userRepository.findByEmail(email)
                        .orElse(null);
                if (user != null) {
                    username = user.getName();
                    projects = projectService.getUserProjects(user);
//                    log.info("사용자 {}의 프로젝트 개수: {}", email, projects.size());
                }
            }
        }

        if (email != null) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                username = user.getName();
                projects = projectService.getUserProjects(user);
                model.addAttribute("user", user); // 추가: 템플릿에서 비교할 때 사용
            }
        }
        model.addAttribute("username", username);
        model.addAttribute("projects", projects);
        return "index";
    }
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}
