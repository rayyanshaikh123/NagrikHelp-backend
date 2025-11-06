package com.nagrikHelp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from:No-Reply <no-reply@nagrikhelp.org>}")
    private String fromAddr;

    public EmailService(@Nullable JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean isEnabled() {
        return this.mailSender != null;
    }

    public void sendEmail(String to, String subject, String htmlBody) throws Exception {
        if (!isEnabled() || to == null || to.isBlank()) return;
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
        helper.setText(htmlBody == null ? "" : htmlBody, true);
        helper.setTo(to.trim());
        helper.setSubject(subject == null ? "Notification" : subject);
        helper.setFrom(fromAddr);
        mailSender.send(message);
    }

    public static String buildEmailBody(String userName, String issueTitle, String issueStatus, String shortMessage) {
        String safeUser = userName == null ? "User" : userName;
        return "<html><body>"
                + "<p>Hi " + escapeHtml(safeUser) + ",</p>"
                + "<p>" + escapeHtml(shortMessage == null ? "" : shortMessage) + "</p>"
                + "<p><strong>Issue:</strong> " + escapeHtml(issueTitle == null ? "" : issueTitle) + "</p>"
                + "<p><strong>Status:</strong> " + escapeHtml(issueStatus == null ? "" : issueStatus) + "</p>"
                + "<p>Thanks,<br/>NagrikHelp Team</p>"
                + "</body></html>";
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }
}
