package com.byys.backend_otp.service;

import com.byys.backend_otp.dto.FeedbackRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;
// EmailService.java
@Service
public class EmailService {

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }
    @Async
    public void sendWelcomeEmail(String toEmail, String fullName, String membershipId) {
        Context context = new Context();
        context.setVariable("name", fullName);
        context.setVariable("membershipId", membershipId);

        String htmlContent = templateEngine.process("welcome-email", context);

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to BYVS Family!");
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            // Log error
        }
    }
    @Async
    public void sendOfficeBearerApprovalEmail(String toEmail, String fullName) {
        Context context = new Context();
        context.setVariable("name", fullName);

        String htmlContent = templateEngine.process("office-bearer-approval", context);

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Congratulations! Your Office Bearer Application Approved");
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            // Log error
        }
    }

    @Async
    public void sendFeedback(FeedbackRequest feedbackRequest) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(fromEmail);
            helper.setSubject("New Feedback Received");

            Context context = new Context();
            context.setVariable("name", feedbackRequest.getName());
            context.setVariable("email", feedbackRequest.getEmail());
            context.setVariable("phone", feedbackRequest.getPhone());
            context.setVariable("subject", feedbackRequest.getSubject());
            context.setVariable("message", feedbackRequest.getMessage());

            String htmlContent = templateEngine.process("feedback-email", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send feedback email: " + e.getMessage());
        }
    }
}