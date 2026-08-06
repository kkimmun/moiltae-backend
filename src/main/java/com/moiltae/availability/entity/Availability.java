package com.moiltae.availability.entity;

import java.time.LocalDateTime;

import com.moiltae.member.entity.Member;
import com.moiltae.room.entity.Room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "moiltae_availabilities",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mt_avail_member_time",
                columnNames = {"room_id", "member_id", "available_at"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Availability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "availability_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime startAt;

    // 기존 1시간 단위 데이터와의 호환을 위해 DB 컬럼은 nullable로 유지한다.
    @Column(name = "end_at")
    private LocalDateTime endAt;

    private Availability(Room room, Member member, LocalDateTime startAt, LocalDateTime endAt) {
        this.room = room;
        this.member = member;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static Availability create(
            Room room,
            Member member,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        return new Availability(room, member, startAt, endAt);
    }

    public LocalDateTime getEndAt() {
        return endAt == null ? startAt.plusHours(1) : endAt;
    }
}
