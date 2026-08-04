package com.moiltae.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moiltae.auth.dto.AuthDto;
import com.moiltae.auth.email.service.EmailVerificationService;
import com.moiltae.auth.service.AuthService;
import com.moiltae.global.common.ApiResponse;
import com.moiltae.member.dto.MemberDto;
import com.moiltae.member.service.MemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final MemberService memberService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/email-verifications")
    public ResponseEntity<ApiResponse<AuthDto.EmailCodeResponse>> requestEmailCode(
            @Valid @RequestBody AuthDto.EmailCodeRequest request
    ) {
        AuthDto.EmailCodeResponse data = emailVerificationService.requestCode(request);
        return ResponseEntity.ok(ApiResponse.success(
                "EMAIL_VERIFICATION_CODE_SENT",
                "이메일 인증번호를 발송했습니다.",
                data
        ));
    }

    @PostMapping("/email-verifications/confirm")
    public ResponseEntity<ApiResponse<AuthDto.EmailCodeConfirmResponse>> confirmEmailCode(
            @Valid @RequestBody AuthDto.EmailCodeConfirmRequest request
    ) {
        AuthDto.EmailCodeConfirmResponse data = emailVerificationService.confirmCode(request);
        return ResponseEntity.ok(ApiResponse.success(
                "EMAIL_VERIFICATION_SUCCEEDED",
                "이메일 인증이 완료되었습니다.",
                data
        ));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<MemberDto.Response>> signup(
            @Valid @RequestBody MemberDto.SignupRequest request
    ) {
        MemberDto.Response data = memberService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("MEMBER_CREATED", "회원가입이 완료되었습니다.", data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request
    ) {
        AuthDto.LoginResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("LOGIN_SUCCEEDED", "로그인했습니다.", data));
    }
}
