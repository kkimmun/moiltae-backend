package com.moiltae.availability.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.moiltae.room.dto.RoomDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AvailabilityDto {
    private AvailabilityDto() {
    }

    public record TimeRange(
            @NotNull(message = "시작 시간은 필수입니다.")
            LocalDateTime startAt,

            @NotNull(message = "종료 시간은 필수입니다.")
            LocalDateTime endAt
    ) {
    }

    public record SaveRequest(
            @NotNull(message = "가능 시간 목록은 필수입니다.")
            @Size(max = 100, message = "가능 시간 구간은 최대 100개까지 입력할 수 있습니다.")
            List<@Valid @NotNull(message = "가능 시간 구간에는 null을 입력할 수 없습니다.") TimeRange> ranges
    ) {
    }

    public record MyResponse(
            Long roomId,
            Long memberId,
            int savedCount,
            List<TimeRange> ranges
    ) {
    }

    public record SubmissionStatusResponse(
            Long roomId,
            int totalMemberCount,
            int submittedMemberCount,
            int unsubmittedMemberCount,
            List<RoomDto.MemberResponse> unsubmittedMembers
    ) {
    }

    public record ResultRange(
            LocalDateTime startAt,
            LocalDateTime endAt,
            int availableMemberCount,
            boolean allMembersAvailable,
            List<RoomDto.MemberResponse> members
    ) {
    }

    public record ResultResponse(
            Long roomId,
            long totalMemberCount,
            List<@Valid ResultRange> ranges
    ) {
    }
}
