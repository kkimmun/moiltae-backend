package com.moiltae.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moiltae.global.common.ApiResponse;
import com.moiltae.global.security.CustomUserDetails;
import com.moiltae.member.dto.MemberDto;
import com.moiltae.member.service.MemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberDto.Response>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MemberDto.Response data = memberService.getMyProfile(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("MEMBER_FOUND", "내 정보를 조회했습니다.", data));
    }
}

