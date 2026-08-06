package com.moiltae.room.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.moiltae.member.entity.Member;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "moiltae_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "closes_at", nullable = false)
    private LocalDateTime closesAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "close_type", length = 20)
    private CloseType closeType;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Room(
            Member owner,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime closesAt,
            LocalDateTime createdAt
    ) {
        this.owner = owner;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.closesAt = closesAt;
        this.status = RoomStatus.OPEN;
        this.createdAt = createdAt;
    }

    public static Room create(
            Member owner,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime closesAt,
            LocalDateTime createdAt
    ) {
        return new Room(owner, title, startDate, endDate, closesAt, createdAt);
    }
}
