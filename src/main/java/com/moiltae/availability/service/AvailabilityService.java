package com.moiltae.availability.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moiltae.availability.dto.AvailabilityDto;
import com.moiltae.availability.entity.Availability;
import com.moiltae.availability.repository.AvailabilityRepository;
import com.moiltae.global.exception.BusinessException;
import com.moiltae.global.exception.ErrorCode;
import com.moiltae.member.entity.Member;
import com.moiltae.member.service.MemberService;
import com.moiltae.room.dto.RoomDto;
import com.moiltae.room.entity.CloseType;
import com.moiltae.room.entity.Room;
import com.moiltae.room.entity.RoomMember;
import com.moiltae.room.entity.RoomStatus;
import com.moiltae.room.repository.RoomMemberRepository;
import com.moiltae.room.repository.RoomRepository;
import com.moiltae.room.service.RoomAccessService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvailabilityService {
    private final AvailabilityRepository availabilityRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomRepository roomRepository;
    private final MemberService memberService;
    private final RoomAccessService roomAccessService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AvailabilityDto.MyResponse findMine(Long roomId, Long memberId) {
        roomAccessService.requireMember(roomId, memberId);
        List<AvailabilityDto.TimeRange> ranges = availabilityRepository
                .findAllByRoom_IdAndMember_IdOrderByStartAtAsc(roomId, memberId)
                .stream()
                .map(this::toTimeRange)
                .toList();
        return new AvailabilityDto.MyResponse(roomId, memberId, ranges.size(), ranges);
    }

    @Transactional
    public AvailabilityDto.MyResponse saveMine(
            Long roomId,
            Long memberId,
            AvailabilityDto.SaveRequest request
    ) {
        Room room = roomAccessService.requireMember(roomId, memberId);
        roomAccessService.requireOpen(room);
        validateRanges(room, request.ranges());

        Member member = memberService.getMember(memberId);
        List<AvailabilityDto.TimeRange> normalizedRanges = normalizeRanges(request.ranges());
        availabilityRepository.deleteMine(roomId, memberId);

        List<Availability> availabilities = normalizedRanges.stream()
                .map(range -> Availability.create(room, member, range.startAt(), range.endAt()))
                .toList();
        availabilityRepository.saveAll(availabilities);
        return new AvailabilityDto.MyResponse(
                roomId,
                memberId,
                normalizedRanges.size(),
                normalizedRanges
        );
    }

    @Transactional(readOnly = true)
    public AvailabilityDto.SubmissionStatusResponse findSubmissionStatus(
            Long roomId,
            Long ownerId
    ) {
        roomAccessService.requireOwner(roomId, ownerId);
        List<RoomMember> roomMembers = roomMemberRepository.findRoomMembers(roomId);
        Set<Long> submittedMemberIds = new HashSet<>(
                availabilityRepository.findSubmittedMemberIds(roomId)
        );
        List<RoomDto.MemberResponse> unsubmittedMembers = roomMembers.stream()
                .map(RoomMember::getMember)
                .filter(member -> !submittedMemberIds.contains(member.getId()))
                .map(member -> new RoomDto.MemberResponse(member.getId(), member.getName()))
                .toList();

        int totalMemberCount = roomMembers.size();
        int submittedMemberCount = totalMemberCount - unsubmittedMembers.size();
        return new AvailabilityDto.SubmissionStatusResponse(
                roomId,
                totalMemberCount,
                submittedMemberCount,
                unsubmittedMembers.size(),
                unsubmittedMembers
        );
    }

    @Transactional
    public AvailabilityDto.ResultResponse findResult(Long roomId, Long memberId) {
        Room room = roomAccessService.requireMember(roomId, memberId);
        if (room.getStatus() == RoomStatus.OPEN) {
            LocalDateTime now = LocalDateTime.now(clock);
            if (room.getClosesAt().isAfter(now)) {
                throw new BusinessException(ErrorCode.ROOM_NOT_CLOSED);
            }
            roomRepository.closeIfOpen(
                    roomId,
                    RoomStatus.OPEN,
                    RoomStatus.CLOSED,
                    CloseType.AUTO,
                    now
            );
        }

        long totalMemberCount = roomMemberRepository.countByRoom_Id(roomId);
        List<OverlapSegment> segments = calculateSegments(
                availabilityRepository.findRoomAvailabilities(roomId)
        );
        int maxCount = segments.stream()
                .mapToInt(segment -> segment.members().size())
                .max()
                .orElse(0);
        if (maxCount == 0) {
            return new AvailabilityDto.ResultResponse(roomId, totalMemberCount, List.of());
        }

        List<OverlapSegment> maximumSegments = segments.stream()
                .filter(segment -> segment.members().size() == maxCount)
                .toList();
        List<AvailabilityDto.ResultRange> resultRanges = mergeAdjacentSegments(maximumSegments)
                .stream()
                .map(segment -> new AvailabilityDto.ResultRange(
                        segment.startAt(),
                        segment.endAt(),
                        segment.members().size(),
                        segment.members().size() == totalMemberCount,
                        List.copyOf(segment.members().values())
                ))
                .toList();
        return new AvailabilityDto.ResultResponse(roomId, totalMemberCount, resultRanges);
    }

    private List<OverlapSegment> calculateSegments(List<Availability> availabilities) {
        TreeSet<LocalDateTime> boundaries = new TreeSet<>();
        availabilities.forEach(availability -> {
            boundaries.add(availability.getStartAt());
            boundaries.add(availability.getEndAt());
        });

        List<LocalDateTime> points = new ArrayList<>(boundaries);
        List<OverlapSegment> segments = new ArrayList<>();
        for (int index = 0; index < points.size() - 1; index++) {
            LocalDateTime startAt = points.get(index);
            LocalDateTime endAt = points.get(index + 1);
            Map<Long, RoomDto.MemberResponse> members = new LinkedHashMap<>();

            for (Availability availability : availabilities) {
                if (!availability.getStartAt().isAfter(startAt)
                        && !availability.getEndAt().isBefore(endAt)) {
                    Member member = availability.getMember();
                    members.putIfAbsent(
                            member.getId(),
                            new RoomDto.MemberResponse(member.getId(), member.getName())
                    );
                }
            }
            if (!members.isEmpty()) {
                segments.add(new OverlapSegment(startAt, endAt, members));
            }
        }
        return segments;
    }

    private List<OverlapSegment> mergeAdjacentSegments(List<OverlapSegment> segments) {
        List<OverlapSegment> merged = new ArrayList<>();
        for (OverlapSegment current : segments) {
            if (!merged.isEmpty()) {
                OverlapSegment previous = merged.get(merged.size() - 1);
                if (previous.endAt().equals(current.startAt())
                        && previous.members().keySet().equals(current.members().keySet())) {
                    merged.set(
                            merged.size() - 1,
                            new OverlapSegment(previous.startAt(), current.endAt(), previous.members())
                    );
                    continue;
                }
            }
            merged.add(current);
        }
        return merged;
    }

    private void validateRanges(Room room, List<AvailabilityDto.TimeRange> ranges) {
        for (AvailabilityDto.TimeRange range : ranges) {
            LocalDateTime startAt = range.startAt();
            LocalDateTime endAt = range.endAt();
            if (!startAt.isBefore(endAt)) {
                throw new BusinessException(ErrorCode.INVALID_AVAILABILITY_RANGE);
            }
            if (!startAt.toLocalDate().equals(endAt.toLocalDate())) {
                throw new BusinessException(ErrorCode.RANGE_MUST_BE_SAME_DAY);
            }
            if (startAt.toLocalDate().isBefore(room.getStartDate())
                    || endAt.toLocalDate().isAfter(room.getEndDate())) {
                throw new BusinessException(ErrorCode.RANGE_OUT_OF_ROOM_RANGE);
            }
        }
    }

    private List<AvailabilityDto.TimeRange> normalizeRanges(
            List<AvailabilityDto.TimeRange> ranges
    ) {
        List<AvailabilityDto.TimeRange> sorted = ranges.stream()
                .sorted(Comparator.comparing(AvailabilityDto.TimeRange::startAt)
                        .thenComparing(AvailabilityDto.TimeRange::endAt))
                .toList();
        List<AvailabilityDto.TimeRange> normalized = new ArrayList<>();

        for (AvailabilityDto.TimeRange current : sorted) {
            if (!normalized.isEmpty()) {
                AvailabilityDto.TimeRange previous = normalized.get(normalized.size() - 1);
                if (previous.startAt().toLocalDate().equals(current.startAt().toLocalDate())
                        && !current.startAt().isAfter(previous.endAt())) {
                    LocalDateTime mergedEndAt = previous.endAt().isAfter(current.endAt())
                            ? previous.endAt()
                            : current.endAt();
                    normalized.set(
                            normalized.size() - 1,
                            new AvailabilityDto.TimeRange(previous.startAt(), mergedEndAt)
                    );
                    continue;
                }
            }
            normalized.add(current);
        }
        return normalized;
    }

    private AvailabilityDto.TimeRange toTimeRange(Availability availability) {
        return new AvailabilityDto.TimeRange(
                availability.getStartAt(),
                availability.getEndAt()
        );
    }

    private record OverlapSegment(
            LocalDateTime startAt,
            LocalDateTime endAt,
            Map<Long, RoomDto.MemberResponse> members
    ) {
    }
}
