package com.moiltae.room.entity;

import java.time.LocalDateTime;

import com.moiltae.member.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "room_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMember {
    @EmbeddedId
    private RoomMemberId id;

    @MapsId("roomId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @MapsId("memberId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    private RoomMember(Room room, Member member, LocalDateTime joinedAt) {
        this.id = new RoomMemberId(room.getId(), member.getId());
        this.room = room;
        this.member = member;
        this.joinedAt = joinedAt;
    }

    public static RoomMember create(Room room, Member member, LocalDateTime joinedAt) {
        return new RoomMember(room, member, joinedAt);
    }
}

