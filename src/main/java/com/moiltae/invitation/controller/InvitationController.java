package com.moiltae.invitation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moiltae.global.common.ApiResponse;
import com.moiltae.global.exception.BusinessException;
import com.moiltae.global.exception.ErrorCode;
import com.moiltae.global.security.CustomUserDetails;
import com.moiltae.invitation.dto.InvitationDto;
import com.moiltae.invitation.entity.InvitationStatus;
import com.moiltae.invitation.service.InvitationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InvitationController {
    private final InvitationService invitationService;

    @GetMapping("/invitations")
    public ResponseEntity<ApiResponse<List<InvitationDto.Response>>> findPending(
            @RequestParam(name = "status", defaultValue = "PENDING") InvitationStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (status != InvitationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "MVP에서는 대기 중인 초대만 조회할 수 있습니다.");
        }
        List<InvitationDto.Response> data = invitationService.findPending(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("INVITATIONS_FOUND", "받은 초대를 조회했습니다.", data));
    }

    @PostMapping("/rooms/{roomId}/invitations")
    public ResponseEntity<ApiResponse<InvitationDto.Response>> invite(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody InvitationDto.CreateRequest request
    ) {
        InvitationDto.Response data = invitationService.invite(roomId, userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("INVITATION_CREATED", "회원을 초대했습니다.", data));
    }

    @PatchMapping("/invitations/{invitationId}")
    public ResponseEntity<ApiResponse<InvitationDto.Response>> respond(
            @PathVariable("invitationId") Long invitationId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody InvitationDto.RespondRequest request
    ) {
        InvitationDto.Response data = invitationService.respond(invitationId, userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.success("INVITATION_RESPONDED", "초대에 응답했습니다.", data));
    }
}
