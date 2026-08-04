package com.moiltae.room.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moiltae.room.entity.CloseType;
import com.moiltae.room.entity.RoomStatus;
import com.moiltae.room.repository.RoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomClosingScheduler {
    private final RoomRepository roomRepository;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${room.close-interval-ms}")
    @Transactional
    public void closeExpiredRooms() {
        LocalDateTime now = LocalDateTime.now(clock);
        int closedCount = roomRepository.closeExpiredRooms(
                RoomStatus.OPEN,
                RoomStatus.CLOSED,
                CloseType.AUTO,
                now
        );
        if (closedCount > 0) {
            log.info("자동 마감 완료: {}개 방", closedCount);
        }
    }
}
