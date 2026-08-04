package com.moiltae.invitation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moiltae.invitation.entity.Invitation;
import com.moiltae.invitation.entity.InvitationStatus;
import com.moiltae.room.entity.RoomStatus;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByRoom_IdAndInvitee_Id(Long roomId, Long inviteeId);

    @Query("""
            select i
              from Invitation i
              join fetch i.room r
              join fetch r.owner
             where i.invitee.id = :inviteeId
               and i.status = :invitationStatus
               and r.status = :roomStatus
             order by i.requestedAt desc
            """)
    List<Invitation> findPendingOpenRoomInvitations(
            @Param("inviteeId") Long inviteeId,
            @Param("invitationStatus") InvitationStatus invitationStatus,
            @Param("roomStatus") RoomStatus roomStatus
    );
}

