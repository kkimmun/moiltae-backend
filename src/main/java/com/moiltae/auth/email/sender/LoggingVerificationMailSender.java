package com.moiltae.auth.email.sender;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "email.verification.smtp-enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class LoggingVerificationMailSender implements VerificationMailSender {
    @Override
    public void sendVerificationCode(String email, String code, long expiresInSeconds) {
        log.warn(
                "[LOCAL EMAIL VERIFICATION] email={}, code={}, expiresInSeconds={}",
                email,
                code,
                expiresInSeconds
        );
    }
}
