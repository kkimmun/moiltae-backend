package com.moiltae.room.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.moiltae.room.entity.CloseType;
import com.moiltae.room.entity.RoomStatus;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class RoomDto {
    private RoomDto() {
    }

    public record CreateRequest(
            @NotBlank(message = "방 제목은 필수입니다.")
            @Size(max = 100, message = "방 제목은 100자 이하여야 합니다.")
            String title,

            @NotNull(message = "시작일은 필수입니다.")
            LocalDate startDate,

            @NotNull(message = "종료일은 필수입니다.")
            LocalDate endDate,

            @NotNull(message = "의견 마감시각은 필수입니다.")
            @Future(message = "의견 마감시각은 현재보다 늦어야 합니다.")
            LocalDateTime closesAt
    ) {
    }

    public record MemberResponse(Long memberId, String name) {
    }

    public record SummaryResponse(
            Long roomId,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime closesAt,
            RoomStatus status,
            boolean owner,
            long memberCount
    ) {
    }

    public record DetailResponse(
            Long roomId,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime closesAt,
            RoomStatus status,
            CloseType closeType,
            LocalDateTime closedAt,
            Long ownerId,
            boolean owner,
            List<MemberResponse> members
    ) {
    }

    public record CloseResponse(
            Long roomId,
            RoomStatus status,
            CloseType closeType,
            LocalDateTime closedAt
    ) {
    }
}

