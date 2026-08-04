package com.moiltae.room.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moiltae.room.entity.RoomMember;
import com.moiltae.room.entity.RoomMemberId;

public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {
    boolean existsByRoom_IdAndMember_Id(Long roomId, Long memberId);

    long countByRoom_Id(Long roomId);

    @Query("""
            select rm
              from RoomMember rm
              join fetch rm.room r
              join fetch r.owner
             where rm.member.id = :memberId
             order by r.createdAt desc
            """)
    List<RoomMember> findMyRooms(@Param("memberId") Long memberId);

    @Query("""
            select rm
              from RoomMember rm
              join fetch rm.member
             where rm.room.id = :roomId
             order by rm.joinedAt asc
            """)
    List<RoomMember> findRoomMembers(@Param("roomId") Long roomId);
}

