package com.moiltae.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class MemberDto {
    private MemberDto() {
    }

    public record SignupRequest(
            @NotBlank(message = "아이디는 필수입니다.")
            @Pattern(regexp = "^[a-z0-9]{4,50}$", message = "아이디는 영문 소문자와 숫자로 4~50자여야 합니다.")
            String loginId,

            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            @Size(max = 254, message = "이메일은 254자 이하여야 합니다.")
            String email,

            @NotBlank(message = "이름은 필수입니다.")
            @Size(min = 2, max = 30, message = "이름은 2~30자여야 합니다.")
            String name,

            @NotBlank(message = "비밀번호는 필수입니다.")
            @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
            String password,

            @NotBlank(message = "비밀번호 확인은 필수입니다.")
            @Size(min = 8, max = 64, message = "비밀번호 확인은 8~64자여야 합니다.")
            String passwordConfirm,

            @NotBlank(message = "이메일 인증을 완료해 주세요.")
            @Size(max = 50, message = "이메일 인증 정보가 올바르지 않습니다.")
            String emailVerificationToken
    ) {
    }

    public record Response(Long memberId, String loginId, String name) {
    }
}
