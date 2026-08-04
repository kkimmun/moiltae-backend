package com.moiltae.room.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MemberService memberService;
    private final RoomAccessService roomAccessService;
    private final Clock clock;

    @Transactional
    public RoomDto.DetailResponse create(Long memberId, RoomDto.CreateRequest request) {
        validateCreateRequest(request);
        Member owner = memberService.getMember(memberId);
        LocalDateTime now = LocalDateTime.now(clock);
        Room room = roomRepository.save(Room.create(
                owner,
                request.title().trim(),
                request.startDate(),
                request.endDate(),
                request.closesAt(),
                now
        ));
        RoomMember ownerMembership = RoomMember.create(room, owner, now);
        roomMemberRepository.save(ownerMembership);
        return toDetail(room, memberId, List.of(ownerMembership));
    }

    @Transactional(readOnly = true)
    public List<RoomDto.SummaryResponse> findMyRooms(Long memberId) {
        return roomMemberRepository.findMyRooms(memberId).stream()
                .map(roomMember -> toSummary(roomMember.getRoom(), memberId))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomDto.DetailResponse findDetail(Long roomId, Long memberId) {
        Room room = roomAccessService.requireMember(roomId, memberId);
        return toDetail(room, memberId, roomMemberRepository.findRoomMembers(roomId));
    }

    @Transactional
    public RoomDto.CloseResponse closeManually(Long roomId, Long memberId) {
        Room room = roomAccessService.requireOwner(roomId, memberId);
        roomAccessService.requireOpen(room);
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = roomRepository.closeIfOpen(
                roomId,
                RoomStatus.OPEN,
                RoomStatus.CLOSED,
                CloseType.MANUAL,
                now
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.ROOM_ALREADY_CLOSED);
        }
        return new RoomDto.CloseResponse(roomId, RoomStatus.CLOSED, CloseType.MANUAL, now);
    }

    private void validateCreateRequest(RoomDto.CreateRequest request) {
        if (request.startDate().isAfter(request.endDate())
                || ChronoUnit.DAYS.between(request.startDate(), request.endDate()) > 13) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime latestCloseTime = request.endDate().plusDays(1).atStartOfDay();
        if (!request.closesAt().isAfter(now) || !request.closesAt().isBefore(latestCloseTime)) {
            throw new BusinessException(ErrorCode.INVALID_CLOSE_TIME);
        }
    }

    private RoomDto.SummaryResponse toSummary(Room room, Long currentMemberId) {
        return new RoomDto.SummaryResponse(
                room.getId(),
                room.getTitle(),
                room.getStartDate(),
                room.getEndDate(),
                room.getClosesAt(),
                room.getStatus(),
                room.getOwner().getId().equals(currentMemberId),
                roomMemberRepository.countByRoom_Id(room.getId())
        );
    }

    private RoomDto.DetailResponse toDetail(Room room, Long currentMemberId, List<RoomMember> roomMembers) {
        List<RoomDto.MemberResponse> members = roomMembers.stream()
                .map(roomMember -> new RoomDto.MemberResponse(
                        roomMember.getMember().getId(),
                        roomMember.getMember().getName()
                ))
                .toList();
        return new RoomDto.DetailResponse(
                room.getId(),
                room.getTitle(),
                room.getStartDate(),
                room.getEndDate(),
                room.getClosesAt(),
                room.getStatus(),
                room.getCloseType(),
                room.getClosedAt(),
                room.getOwner().getId(),
                room.getOwner().getId().equals(currentMemberId),
                members
        );
    }
}
