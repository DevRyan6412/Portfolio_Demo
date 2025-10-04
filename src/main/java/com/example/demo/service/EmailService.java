package com.example.demo.service;

import com.example.demo.domain.entity.Invitation;
import com.example.demo.domain.entity.Project;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    // 보내는 사람 이메일 (application.properties에 설정)
    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendInvitationEmail(Invitation invitation, Project project) {
        String subject = "프로젝트 초대 메일";
//        String url = "http://goottproject2-env.eba-buwhqs5i.us-east-2.elasticbeanstalk.com/projects/" + project.getId() +
        //AWS
        String url = "http://alb-demo-public-1294612864.ap-northeast-2.elb.amazonaws.com/projects/" + project.getId() +
        //Local
//        String url = "http://localhost:5000/projects/" + project.getId() +
                "/management/invitations/accept?token=" + invitation.getToken();
        // HTML 본문에 a 태그를 사용하여 클릭 가능한 링크 생성
        String text = "<p>안녕하세요!</p>" +
                "<p>아래 링크를 클릭하여 초대를 수락하세요:</p>" +
                "<p><a href=\"" + url + "\">초대 수락하기</a></p>" +
                "<p>감사합니다.</p>";

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(invitation.getEmail());
            helper.setSubject(subject);
            helper.setText(text, true); // 두 번째 파라미터 true : HTML 형식 사용

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("메일 전송 실패", e);
            // 필요한 예외 처리 추가
        }
    }
}