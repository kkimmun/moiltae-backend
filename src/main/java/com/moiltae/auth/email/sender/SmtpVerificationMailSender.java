package com.moiltae.auth.email.sender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "email.verification.smtp-enabled",
        havingValue = "true"
)
public class SmtpVerificationMailSender implements VerificationMailSender {
    private final JavaMailSender mailSender;

    @Value("${email.verification.from:}")
    private String from;

    @Override
    public void sendVerificationCode(String email, String code, long expiresInSeconds) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(email);
        message.setSubject("[모일때] 이메일 인증번호");
        message.setText("""
                모일때 회원가입 인증번호는 %s입니다.

                인증번호는 %d분 동안 유효합니다.
                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(code, Math.max(1, expiresInSeconds / 60)));
        mailSender.send(message);
    }
}
