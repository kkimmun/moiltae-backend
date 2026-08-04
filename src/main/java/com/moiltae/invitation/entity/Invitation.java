package com.moiltae.invitation.entity;

import java.time.LocalDateTime;

import com.moiltae.member.entity.Member;
import com.moiltae.room.entity.Room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "invitations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_invitations_room_invitee",
                columnNames = {"room_id", "invitee_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitee_id", nullable = false)
    private Member invitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    private Invitation(Room room, Member invitee, LocalDateTime requestedAt) {
        this.room = room;
        this.invitee = invitee;
        this.status = InvitationStatus.PENDING;
        this.requestedAt = requestedAt;
    }

    public static Invitation create(Room room, Member invitee, LocalDateTime requestedAt) {
        return new Invitation(room, invitee, requestedAt);
    }

    public void reopen(LocalDateTime requestedAt) {
        this.status = InvitationStatus.PENDING;
        this.requestedAt = requestedAt;
        this.respondedAt = null;
    }

    public void accept(LocalDateTime respondedAt) {
        this.status = InvitationStatus.ACCEPTED;
        this.respondedAt = respondedAt;
    }

    public void reject(LocalDateTime respondedAt) {
        this.status = InvitationStatus.REJECTED;
        this.respondedAt = respondedAt;
    }
}

