package com.moiltae.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.moiltae.auth.dto.AuthDto;
import com.moiltae.global.exception.BusinessException;
import com.moiltae.global.exception.ErrorCode;
import com.moiltae.global.security.CustomUserDetails;
import com.moiltae.global.security.JwtTokenProvider;
import com.moiltae.member.dto.MemberDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.loginId(), request.password())
            );
            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
            String accessToken = jwtTokenProvider.createAccessToken(principal);
            MemberDto.Response member = new MemberDto.Response(
                    principal.getMemberId(),
                    principal.getUsername(),
                    principal.getName()
            );
            return new AuthDto.LoginResponse(
                    accessToken,
                    "Bearer",
                    jwtTokenProvider.getExpirationSeconds(),
                    member
            );
        } catch (AuthenticationException exception) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }
}
