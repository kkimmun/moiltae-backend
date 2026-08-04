package com.moiltae.auth.dto;

import com.moiltae.member.dto.MemberDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDto {
    private AuthDto() {
    }

    public record LoginRequest(
            @NotBlank(message = "아이디는 필수입니다.") String loginId,
            @NotBlank(message = "비밀번호는 필수입니다.") String password
    ) {
    }

    public record LoginResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            MemberDto.Response member
    ) {
    }

    public record EmailCodeRequest(
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            @Size(max = 254, message = "이메일은 254자 이하여야 합니다.")
            String email
    ) {
    }

    public record EmailCodeResponse(
            String email,
            long expiresInSeconds,
            long resendAfterSeconds
    ) {
    }

    public record EmailCodeConfirmRequest(
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            @NotBlank(message = "인증번호는 필수입니다.")
            @Pattern(regexp = "^[0-9]{6}$", message = "인증번호는 숫자 6자리여야 합니다.")
            String code
    ) {
    }

    public record EmailCodeConfirmResponse(
            String email,
            String verificationToken,
            long expiresInSeconds
    ) {
    }
}
