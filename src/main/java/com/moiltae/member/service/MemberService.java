package com.moiltae.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moiltae.auth.email.service.EmailVerificationService;
import com.moiltae.global.exception.BusinessException;
import com.moiltae.global.exception.ErrorCode;
import com.moiltae.member.dto.MemberDto;
import com.moiltae.member.entity.Member;
import com.moiltae.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public MemberDto.Response signup(MemberDto.SignupRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        emailVerificationService.consume(
                normalizedEmail,
                request.emailVerificationToken()
        );

        Member member = Member.create(
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                normalizedEmail
        );
        return toResponse(memberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public MemberDto.Response getMyProfile(Long memberId) {
        return toResponse(getMember(memberId));
    }

    @Transactional(readOnly = true)
    public Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Member getMemberByLoginId(String loginId, ErrorCode notFoundError) {
        return memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(notFoundError));
    }

    public MemberDto.Response toResponse(Member member) {
        return new MemberDto.Response(member.getId(), member.getLoginId(), member.getName());
    }
}
