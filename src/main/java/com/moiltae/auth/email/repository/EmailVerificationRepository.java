package com.moiltae.auth.email.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moiltae.auth.email.entity.EmailVerification;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmail(String email);

    Optional<EmailVerification> findByEmailAndVerificationToken(
            String email,
            String verificationToken
    );
}
