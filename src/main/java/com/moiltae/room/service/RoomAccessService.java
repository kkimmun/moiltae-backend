package com.moiltae.room.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moiltae.global.exception.BusinessException;
import com.moiltae.global.exception.ErrorCode;
import com.moiltae.room.entity.Room;
import com.moiltae.room.entity.RoomStatus;
import com.moiltae.room.repository.RoomMemberRepository;
import com.moiltae.room.repository.RoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomAccessService {
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Room getRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Room requireMember(Long roomId, Long memberId) {
        Room room = getRoom(roomId);
        if (!roomMemberRepository.existsByRoom_IdAndMember_Id(roomId, memberId)) {
            throw new BusinessException(ErrorCode.ROOM_ACCESS_DENIED);
        }
        return room;
    }

    @Transactional(readOnly = true)
    public Room requireOwner(Long roomId, Long memberId) {
        Room room = getRoom(roomId);
        if (!room.getOwner().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ROOM_OWNER_REQUIRED);
        }
        return room;
    }

    public void requireOpen(Room room) {
        if (room.getStatus() != RoomStatus.OPEN
                || !room.getClosesAt().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.ROOM_ALREADY_CLOSED);
        }
    }
}
