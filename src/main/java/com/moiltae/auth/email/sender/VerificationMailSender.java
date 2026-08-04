package com.moiltae.auth.email.sender;

public interface VerificationMailSender {
    void sendVerificationCode(String email, String code, long expiresInSeconds);
}
