package com.moiltae.invitation.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moiltae.global.exception.BusinessException;
import com.moiltae.global.exception.ErrorCode;
import com.moiltae.invitation.dto.InvitationDto;
import com.moiltae.invitation.entity.Invitation;
import com.moiltae.invitation.entity.InvitationStatus;
import com.moiltae.invitation.repository.InvitationRepository;
import com.moiltae.member.entity.Member;
import com.moiltae.member.service.MemberService;
import com.moiltae.room.entity.Room;
import com.moiltae.room.entity.RoomMember;
import com.moiltae.room.entity.RoomStatus;
import com.moiltae.room.repository.RoomMemberRepository;
import com.moiltae.room.service.RoomAccessService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationService {
    private final InvitationRepository invitationRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MemberService memberService;
    private final RoomAccessService roomAccessService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<InvitationDto.Response> findPending(Long memberId) {
        return invitationRepository.findPendingOpenRoomInvitations(
                        memberId,
                        InvitationStatus.PENDING,
                        RoomStatus.OPEN
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InvitationDto.Response invite(Long roomId, Long ownerId, InvitationDto.CreateRequest request) {
        Room room = roomAccessService.requireOwner(roomId, ownerId);
        roomAccessService.requireOpen(room);
        Member invitee = memberService.getMemberByLoginId(request.loginId(), ErrorCode.INVITEE_NOT_FOUND);

        if (invitee.getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.CANNOT_INVITE_SELF);
        }
        if (roomMemberRepository.existsByRoom_IdAndMember_Id(roomId, invitee.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_ROOM_MEMBER);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Invitation invitation = invitationRepository.findByRoom_IdAndInvitee_Id(roomId, invitee.getId())
                .map(existing -> reopen(existing, now))
                .orElseGet(() -> Invitation.create(room, invitee, now));
        return toResponse(invitationRepository.save(invitation));
    }

    @Transactional
    public InvitationDto.Response respond(
            Long invitationId,
            Long memberId,
            InvitationDto.RespondRequest request
    ) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));

        if (!invitation.getInvitee().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.INVITATION_ACCESS_DENIED);
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVITATION_ALREADY_RESPONDED);
        }

        roomAccessService.requireOpen(invitation.getRoom());
        LocalDateTime now = LocalDateTime.now(clock);
        if (request.status() == InvitationDto.Decision.ACCEPTED) {
            invitation.accept(now);
            if (!roomMemberRepository.existsByRoom_IdAndMember_Id(invitation.getRoom().getId(), memberId)) {
                roomMemberRepository.save(RoomMember.create(invitation.getRoom(), invitation.getInvitee(), now));
            }
        } else {
            invitation.reject(now);
        }
        return toResponse(invitation);
    }

    private Invitation reopen(Invitation invitation, LocalDateTime now) {
        if (invitation.getStatus() == InvitationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVITATION_ALREADY_PENDING);
        }
        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw new BusinessException(ErrorCode.ALREADY_ROOM_MEMBER);
        }
        invitation.reopen(now);
        return invitation;
    }

    private InvitationDto.Response toResponse(Invitation invitation) {
        Member invitee = invitation.getInvitee();
        return new InvitationDto.Response(
                invitation.getId(),
                invitation.getRoom().getId(),
                invitation.getRoom().getTitle(),
                invitation.getRoom().getOwner().getName(),
                new InvitationDto.InviteeResponse(invitee.getId(), invitee.getLoginId(), invitee.getName()),
                invitation.getStatus(),
                invitation.getRequestedAt(),
                invitation.getRespondedAt()
        );
    }
}

