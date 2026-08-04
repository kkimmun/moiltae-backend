package com.moiltae.auth.email.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moiltae.auth.dto.AuthDto;
import com.moiltae.auth.email.entity.EmailVerification;
import com.moiltae.auth.email.repository.EmailVerificationRepository;
import com.moiltae.auth.email.sender.VerificationMailSender;
import com.moiltae.global.exception.BusinessException;
import com.moiltae.global.exception.ErrorCode;
import com.moiltae.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final EmailVerificationRepository emailVerificationRepository;
    private final MemberRepository memberRepository;
    private final VerificationMailSender verificationMailSender;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${email.verification.code-expiration-seconds:300}")
    private long codeExpirationSeconds;

    @Value("${email.verification.token-expiration-seconds:900}")
    private long tokenExpirationSeconds;

    @Value("${email.verification.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Transactional
    public AuthDto.EmailCodeResponse requestCode(AuthDto.EmailCodeRequest request) {
        String email = normalizeEmail(request.email());
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        EmailVerification existing = emailVerificationRepository.findByEmail(email).orElse(null);
        if (existing != null && !existing.canResend(now, resendCooldownSeconds)) {
            long remaining = existing.remainingCooldownSeconds(now, resendCooldownSeconds);
            throw new BusinessException(
                    ErrorCode.EMAIL_VERIFICATION_RESEND_TOO_SOON,
                    "인증번호는 " + remaining + "초 후 다시 요청할 수 있습니다."
            );
        }

        String code = "%06d".formatted(secureRandom.nextInt(1_000_000));
        String codeHash = passwordEncoder.encode(code);
        LocalDateTime expiresAt = now.plusSeconds(codeExpirationSeconds);

        EmailVerification verification;
        if (existing == null) {
            verification = EmailVerification.create(email, codeHash, expiresAt, now);
        } else {
            existing.refreshCode(codeHash, expiresAt, now);
            verification = existing;
        }

        emailVerificationRepository.save(verification);
        verificationMailSender.sendVerificationCode(email, code, codeExpirationSeconds);
        return new AuthDto.EmailCodeResponse(
                email,
                codeExpirationSeconds,
                resendCooldownSeconds
        );
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthDto.EmailCodeConfirmResponse confirmCode(
            AuthDto.EmailCodeConfirmRequest request
    ) {
        String email = normalizeEmail(request.email());
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE));
        LocalDateTime now = LocalDateTime.now(clock);

        if (verification.getCodeExpiresAt() == null
                || !verification.getCodeExpiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED);
        }
        if (verification.getFailedAttempts() >= 5) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED);
        }
        if (verification.getCodeHash() == null
                || !passwordEncoder.matches(request.code(), verification.getCodeHash())) {
            verification.increaseFailedAttempts();
            if (verification.getFailedAttempts() >= 5) {
                throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED);
            }
            throw new BusinessException(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE);
        }

        String verificationToken = UUID.randomUUID().toString();
        verification.markVerified(
                verificationToken,
                now,
                now.plusSeconds(tokenExpirationSeconds)
        );
        return new AuthDto.EmailCodeConfirmResponse(
                email,
                verificationToken,
                tokenExpirationSeconds
        );
    }

    @Transactional
    public void consume(String rawEmail, String verificationToken) {
        String email = normalizeEmail(rawEmail);
        EmailVerification verification = emailVerificationRepository
                .findByEmailAndVerificationToken(email, verificationToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_REQUIRED));
        LocalDateTime now = LocalDateTime.now(clock);

        if (verification.getUsedAt() != null) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_ALREADY_USED);
        }
        if (verification.getVerifiedAt() == null) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }
        if (verification.getTokenExpiresAt() == null
                || !verification.getTokenExpiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_TOKEN_EXPIRED);
        }
        verification.markUsed(now);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
