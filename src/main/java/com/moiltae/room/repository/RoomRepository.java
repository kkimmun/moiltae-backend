package com.moiltae.room.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moiltae.room.entity.CloseType;
import com.moiltae.room.entity.Room;
import com.moiltae.room.entity.RoomStatus;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Room r
               set r.status = :closedStatus,
                   r.closeType = :closeType,
                   r.closedAt = :closedAt
             where r.id = :roomId
               and r.status = :openStatus
            """)
    int closeIfOpen(
            @Param("roomId") Long roomId,
            @Param("openStatus") RoomStatus openStatus,
            @Param("closedStatus") RoomStatus closedStatus,
            @Param("closeType") CloseType closeType,
            @Param("closedAt") LocalDateTime closedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Room r
               set r.status = :closedStatus,
                   r.closeType = :closeType,
                   r.closedAt = :closedAt
             where r.status = :openStatus
               and r.closesAt <= :closedAt
            """)
    int closeExpiredRooms(
            @Param("openStatus") RoomStatus openStatus,
            @Param("closedStatus") RoomStatus closedStatus,
            @Param("closeType") CloseType closeType,
            @Param("closedAt") LocalDateTime closedAt
    );
}

