package com.moiltae.availability.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moiltae.availability.entity.Availability;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    List<Availability> findAllByRoom_IdAndMember_IdOrderByStartAtAsc(Long roomId, Long memberId);

    @Query("""
            select distinct a.member.id
              from Availability a
             where a.room.id = :roomId
            """)
    List<Long> findSubmittedMemberIds(@Param("roomId") Long roomId);

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from Availability a
             where a.room.id = :roomId
               and a.member.id = :memberId
            """)
    int deleteMine(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

    @Query("""
            select a
              from Availability a
              join fetch a.member
             where a.room.id = :roomId
             order by a.startAt asc, a.endAt asc
            """)
    List<Availability> findRoomAvailabilities(@Param("roomId") Long roomId);
}
