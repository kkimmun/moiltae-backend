package com.moiltae.auth.email.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "email_verifications",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_email_verifications_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_email_verifications_token", columnNames = "verification_token")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_verification_id")
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "code_hash", length = 255)
    private String codeHash;

    @Column(name = "code_expires_at")
    private LocalDateTime codeExpiresAt;

    @Column(name = "verification_token", length = 50)
    private String verificationToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private EmailVerification(
            String email,
            String codeHash,
            LocalDateTime codeExpiresAt,
            LocalDateTime now
    ) {
        this.email = email;
        this.codeHash = codeHash;
        this.codeExpiresAt = codeExpiresAt;
        this.lastSentAt = now;
        this.failedAttempts = 0;
        this.createdAt = now;
    }

    public static EmailVerification create(
            String email,
            String codeHash,
            LocalDateTime codeExpiresAt,
            LocalDateTime now
    ) {
        return new EmailVerification(email, codeHash, codeExpiresAt, now);
    }

    public boolean canResend(LocalDateTime now, long cooldownSeconds) {
        return !lastSentAt.plusSeconds(cooldownSeconds).isAfter(now);
    }

    public long remainingCooldownSeconds(LocalDateTime now, long cooldownSeconds) {
        return Math.max(1, java.time.Duration.between(
                now,
                lastSentAt.plusSeconds(cooldownSeconds)
        ).toSeconds() + 1);
    }

    public void refreshCode(
            String newCodeHash,
            LocalDateTime newCodeExpiresAt,
            LocalDateTime now
    ) {
        this.codeHash = newCodeHash;
        this.codeExpiresAt = newCodeExpiresAt;
        this.verificationToken = null;
        this.tokenExpiresAt = null;
        this.verifiedAt = null;
        this.usedAt = null;
        this.lastSentAt = now;
        this.failedAttempts = 0;
    }

    public void markVerified(
            String token,
            LocalDateTime verifiedAt,
            LocalDateTime tokenExpiresAt
    ) {
        this.codeHash = null;
        this.codeExpiresAt = null;
        this.verificationToken = token;
        this.verifiedAt = verifiedAt;
        this.tokenExpiresAt = tokenExpiresAt;
        this.usedAt = null;
        this.failedAttempts = 0;
    }

    public void increaseFailedAttempts() {
        this.failedAttempts += 1;
    }

    public void markUsed(LocalDateTime now) {
        this.usedAt = now;
    }
}
