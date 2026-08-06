package com.moiltae;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.moiltae.auth.dto.AuthDto;
import com.moiltae.auth.email.sender.VerificationMailSender;
import com.moiltae.auth.email.service.EmailVerificationService;
import com.moiltae.auth.service.AuthService;
import com.moiltae.availability.dto.AvailabilityDto;
import com.moiltae.availability.service.AvailabilityService;
import com.moiltae.global.exception.BusinessException;
import com.moiltae.global.exception.ErrorCode;
import com.moiltae.invitation.dto.InvitationDto;
import com.moiltae.invitation.entity.InvitationStatus;
import com.moiltae.invitation.service.InvitationService;
import com.moiltae.member.dto.MemberDto;
import com.moiltae.member.entity.Member;
import com.moiltae.member.service.MemberService;
import com.moiltae.room.dto.RoomDto;
import com.moiltae.room.entity.CloseType;
import com.moiltae.room.entity.Room;
import com.moiltae.room.entity.RoomMember;
import com.moiltae.room.entity.RoomStatus;
import com.moiltae.room.repository.RoomMemberRepository;
import com.moiltae.room.repository.RoomRepository;
import com.moiltae.room.service.RoomClosingScheduler;
import com.moiltae.room.service.RoomService;

@SpringBootTest
@Import(RoomWorkflowIntegrationTest.MailTestConfiguration.class)
class RoomWorkflowIntegrationTest {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Autowired
    private MemberService memberService;

    @Autowired
    private AuthService authService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private RoomClosingScheduler roomClosingScheduler;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private CapturingVerificationMailSender verificationMailSender;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void schemaUsesOnlyMoiltaePrefixedTableNames() {
        List<String> tableNames = jdbcTemplate.queryForList(
                """
                select table_name
                  from information_schema.tables
                 where table_schema = 'PUBLIC'
                """,
                String.class
        );

        assertThat(tableNames).contains(
                "MOILTAE_MEMBERS",
                "MOILTAE_EMAIL_VERIFICATIONS",
                "MOILTAE_ROOMS",
                "MOILTAE_ROOM_MEMBERS",
                "MOILTAE_INVITATIONS",
                "MOILTAE_AVAILABILITIES"
        );
        assertThat(tableNames).doesNotContain(
                "MEMBERS",
                "EMAIL_VERIFICATIONS",
                "ROOMS",
                "ROOM_MEMBERS",
                "INVITATIONS",
                "AVAILABILITIES"
        );
    }

    @Test
    void invitationOnlyRoomWorkflowCalculatesDisconnectedMaximumOverlapRanges() {
        MemberDto.Response owner = signup("owner01", "방장");
        MemberDto.Response invitee = signup("member01", "참여자");
        MemberDto.Response outsider = signup("outsider01", "외부인");

        LocalDate startDate = LocalDate.now(SEOUL).plusDays(1);
        RoomDto.DetailResponse room = roomService.create(
                owner.memberId(),
                new RoomDto.CreateRequest(
                        "프로젝트 회의",
                        startDate,
                        startDate.plusDays(1),
                        LocalDateTime.now(SEOUL).plusHours(1)
                )
        );

        assertThat(room.status()).isEqualTo(RoomStatus.OPEN);
        assertThat(room.members()).extracting(RoomDto.MemberResponse::memberId)
                .containsExactly(owner.memberId());
        assertThat(roomService.findMyRooms(invitee.memberId())).isEmpty();
        assertThatThrownBy(() -> roomService.findDetail(room.roomId(), outsider.memberId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROOM_ACCESS_DENIED));

        InvitationDto.Response invitation = invitationService.invite(
                room.roomId(),
                owner.memberId(),
                new InvitationDto.CreateRequest(invitee.loginId())
        );
        assertThat(invitation.status()).isEqualTo(InvitationStatus.PENDING);
        assertThat(invitationService.findPending(invitee.memberId())).hasSize(1);

        InvitationDto.Response accepted = invitationService.respond(
                invitation.invitationId(),
                invitee.memberId(),
                new InvitationDto.RespondRequest(InvitationDto.Decision.ACCEPTED)
        );
        assertThat(accepted.status()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(roomService.findMyRooms(invitee.memberId()))
                .extracting(RoomDto.SummaryResponse::roomId)
                .containsExactly(room.roomId());

        AvailabilityDto.SubmissionStatusResponse beforeSubmission =
                availabilityService.findSubmissionStatus(room.roomId(), owner.memberId());
        assertThat(beforeSubmission.totalMemberCount()).isEqualTo(2);
        assertThat(beforeSubmission.submittedMemberCount()).isZero();
        assertThat(beforeSubmission.unsubmittedMemberCount()).isEqualTo(2);
        assertThat(beforeSubmission.unsubmittedMembers())
                .extracting(RoomDto.MemberResponse::memberId)
                .containsExactly(owner.memberId(), invitee.memberId());
        assertThatThrownBy(() -> availabilityService.findSubmissionStatus(
                room.roomId(),
                invitee.memberId()
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROOM_OWNER_REQUIRED));

        LocalDateTime ownerMorningStart = startDate.atTime(9, 0);
        LocalDateTime morningEnd = startDate.atTime(12, 0);
        LocalDateTime inviteeMorningStart = startDate.atTime(10, 0);
        LocalDateTime afternoonStart = startDate.atTime(13, 0);
        LocalDateTime inviteeAfternoonEnd = startDate.atTime(16, 0);
        LocalDateTime ownerAfternoonEnd = startDate.atTime(18, 0);
        LocalDateTime secondDayStart = startDate.plusDays(1).atTime(9, 0);
        LocalDateTime inviteeSecondDayEnd = startDate.plusDays(1).atTime(11, 0);
        LocalDateTime ownerSecondDayEnd = startDate.plusDays(1).atTime(12, 0);

        availabilityService.saveMine(
                room.roomId(),
                owner.memberId(),
                new AvailabilityDto.SaveRequest(List.of(
                        new AvailabilityDto.TimeRange(ownerMorningStart, morningEnd),
                        new AvailabilityDto.TimeRange(afternoonStart, ownerAfternoonEnd),
                        new AvailabilityDto.TimeRange(secondDayStart, ownerSecondDayEnd)
                ))
        );

        AvailabilityDto.SubmissionStatusResponse afterOwnerSubmission =
                availabilityService.findSubmissionStatus(room.roomId(), owner.memberId());
        assertThat(afterOwnerSubmission.submittedMemberCount()).isEqualTo(1);
        assertThat(afterOwnerSubmission.unsubmittedMemberCount()).isEqualTo(1);
        assertThat(afterOwnerSubmission.unsubmittedMembers())
                .extracting(RoomDto.MemberResponse::memberId)
                .containsExactly(invitee.memberId());

        availabilityService.saveMine(
                room.roomId(),
                invitee.memberId(),
                new AvailabilityDto.SaveRequest(List.of(
                        new AvailabilityDto.TimeRange(inviteeMorningStart, morningEnd),
                        new AvailabilityDto.TimeRange(afternoonStart, inviteeAfternoonEnd),
                        new AvailabilityDto.TimeRange(secondDayStart, inviteeSecondDayEnd)
                ))
        );

        AvailabilityDto.SubmissionStatusResponse afterAllSubmissions =
                availabilityService.findSubmissionStatus(room.roomId(), owner.memberId());
        assertThat(afterAllSubmissions.submittedMemberCount()).isEqualTo(2);
        assertThat(afterAllSubmissions.unsubmittedMemberCount()).isZero();
        assertThat(afterAllSubmissions.unsubmittedMembers()).isEmpty();

        assertThatThrownBy(() -> availabilityService.findResult(room.roomId(), invitee.memberId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROOM_NOT_CLOSED));

        RoomDto.CloseResponse closed = roomService.closeManually(room.roomId(), owner.memberId());
        assertThat(closed.status()).isEqualTo(RoomStatus.CLOSED);
        assertThat(closed.closeType()).isEqualTo(CloseType.MANUAL);

        AvailabilityDto.ResultResponse result = availabilityService.findResult(room.roomId(), invitee.memberId());
        assertThat(result.totalMemberCount()).isEqualTo(2);
        assertThat(result.ranges()).hasSize(3);
        assertThat(result.ranges()).extracting(AvailabilityDto.ResultRange::startAt)
                .containsExactly(inviteeMorningStart, afternoonStart, secondDayStart);
        assertThat(result.ranges()).extracting(AvailabilityDto.ResultRange::endAt)
                .containsExactly(morningEnd, inviteeAfternoonEnd, inviteeSecondDayEnd);
        assertThat(result.ranges()).allSatisfy(range -> {
            assertThat(range.availableMemberCount()).isEqualTo(2);
            assertThat(range.allMembersAvailable()).isTrue();
            assertThat(range.members()).extracting(RoomDto.MemberResponse::memberId)
                    .containsExactlyInAnyOrder(owner.memberId(), invitee.memberId());
        });

        assertThatThrownBy(() -> availabilityService.saveMine(
                room.roomId(),
                owner.memberId(),
                new AvailabilityDto.SaveRequest(List.of(
                        new AvailabilityDto.TimeRange(ownerMorningStart, morningEnd)
                ))
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROOM_ALREADY_CLOSED));
    }

    @Test
    void loginIssuesJwtAndRejectsWrongPassword() {
        MemberDto.Response member = signup("loginuser01", "로그인회원");

        assertThatThrownBy(() -> emailVerificationService.requestCode(
                new AuthDto.EmailCodeRequest("loginuser01@example.com")
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_EMAIL));

        assertThatThrownBy(() -> memberService.signup(new MemberDto.SignupRequest(
                "anotherlogin01",
                "loginuser01@example.com",
                "중복이메일",
                "password123!",
                "password123!",
                "invalid-token"
        ))).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_EMAIL));

        MemberDto.SignupRequest mismatchRequest = verifiedSignupRequest(
                "mismatch01",
                "비밀번호불일치"
        );
        assertThatThrownBy(() -> memberService.signup(new MemberDto.SignupRequest(
                mismatchRequest.loginId(),
                mismatchRequest.email(),
                mismatchRequest.name(),
                mismatchRequest.password(),
                "different-password",
                mismatchRequest.emailVerificationToken()
        ))).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH));

        AuthDto.LoginResponse response = authService.login(
                new AuthDto.LoginRequest(member.loginId(), "password123!")
        );

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isPositive();
        assertThat(response.member()).isEqualTo(member);

        assertThatThrownBy(() -> authService.login(
                new AuthDto.LoginRequest(member.loginId(), "wrong-password")
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void schedulerAutomaticallyClosesExpiredRooms() {
        MemberDto.Response ownerResponse = signup("autoowner01", "자동마감방장");
        Member owner = memberService.getMember(ownerResponse.memberId());
        LocalDateTime now = LocalDateTime.now(SEOUL);
        LocalDate eventDate = now.toLocalDate().plusDays(1);

        Room room = roomRepository.save(Room.create(
                owner,
                "자동 마감 검증",
                eventDate,
                eventDate,
                now.minusSeconds(1),
                now.minusMinutes(5)
        ));
        roomMemberRepository.save(RoomMember.create(room, owner, now.minusMinutes(5)));

        roomClosingScheduler.closeExpiredRooms();

        Room closedRoom = roomRepository.findById(room.getId()).orElseThrow();
        assertThat(closedRoom.getStatus()).isEqualTo(RoomStatus.CLOSED);
        assertThat(closedRoom.getCloseType()).isEqualTo(CloseType.AUTO);
        assertThat(closedRoom.getClosedAt()).isNotNull();

        AvailabilityDto.ResultResponse result = availabilityService.findResult(
                room.getId(),
                ownerResponse.memberId()
        );
        assertThat(result.totalMemberCount()).isEqualTo(1);
        assertThat(result.ranges()).isEmpty();
    }

    private MemberDto.Response signup(String loginId, String name) {
        return memberService.signup(verifiedSignupRequest(loginId, name));
    }

    private MemberDto.SignupRequest verifiedSignupRequest(String loginId, String name) {
        String email = loginId + "@example.com";
        emailVerificationService.requestCode(new AuthDto.EmailCodeRequest(email));
        String code = verificationMailSender.latestCode(email);
        AuthDto.EmailCodeConfirmResponse confirmation = emailVerificationService.confirmCode(
                new AuthDto.EmailCodeConfirmRequest(email, code)
        );
        return new MemberDto.SignupRequest(
                loginId,
                email,
                name,
                "password123!",
                "password123!",
                confirmation.verificationToken()
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MailTestConfiguration {
        @Bean
        @Primary
        CapturingVerificationMailSender capturingVerificationMailSender() {
            return new CapturingVerificationMailSender();
        }
    }

    static class CapturingVerificationMailSender implements VerificationMailSender {
        private final Map<String, String> latestCodes = new ConcurrentHashMap<>();

        @Override
        public void sendVerificationCode(String email, String code, long expiresInSeconds) {
            latestCodes.put(email, code);
        }

        String latestCode(String email) {
            return latestCodes.get(email);
        }
    }
}
