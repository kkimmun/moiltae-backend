package com.moiltae.invitation.dto;

import java.time.LocalDateTime;

import com.moiltae.invitation.entity.InvitationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class InvitationDto {
    private InvitationDto() {
    }

    public record CreateRequest(
            @NotBlank(message = "초대할 회원 아이디는 필수입니다.") String loginId
    ) {
    }

    public enum Decision {
        ACCEPTED,
        REJECTED
    }

    public record RespondRequest(
            @NotNull(message = "초대 응답은 필수입니다.") Decision status
    ) {
    }

    public record InviteeResponse(Long memberId, String loginId, String name) {
    }

    public record Response(
            Long invitationId,
            Long roomId,
            String roomTitle,
            String ownerName,
            InviteeResponse invitee,
            InvitationStatus status,
            LocalDateTime requestedAt,
            LocalDateTime respondedAt
    ) {
    }
}

