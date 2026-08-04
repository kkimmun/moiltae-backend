package com.moiltae.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 형식이 올바르지 않습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "일정 기간은 시작일부터 최대 14일까지 설정할 수 있습니다."),
    INVALID_CLOSE_TIME(HttpStatus.BAD_REQUEST, "INVALID_CLOSE_TIME", "의견 마감시각을 확인해 주세요."),
    INVALID_AVAILABILITY_RANGE(HttpStatus.BAD_REQUEST, "INVALID_AVAILABILITY_RANGE", "시작 시간은 종료 시간보다 빨라야 합니다."),
    RANGE_MUST_BE_SAME_DAY(HttpStatus.BAD_REQUEST, "RANGE_MUST_BE_SAME_DAY", "가능 시간의 시작과 종료는 같은 날짜여야 합니다."),
    RANGE_OUT_OF_ROOM_RANGE(HttpStatus.BAD_REQUEST, "RANGE_OUT_OF_ROOM_RANGE", "가능 시간은 방의 일정 범위 안에 있어야 합니다."),
    PASSWORD_CONFIRMATION_MISMATCH(HttpStatus.BAD_REQUEST, "PASSWORD_CONFIRMATION_MISMATCH", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    INVALID_EMAIL_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "INVALID_EMAIL_VERIFICATION_CODE", "이메일 인증번호가 올바르지 않습니다."),
    EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED", "인증번호 입력 횟수를 초과했습니다. 인증번호를 다시 발급해 주세요."),
    EMAIL_VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_CODE_EXPIRED", "이메일 인증번호가 만료되었습니다. 다시 발급해 주세요."),
    EMAIL_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_REQUIRED", "이메일 인증을 완료해 주세요."),
    EMAIL_VERIFICATION_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_TOKEN_EXPIRED", "이메일 인증 유효시간이 만료되었습니다. 다시 인증해 주세요."),
    EMAIL_VERIFICATION_ALREADY_USED(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_ALREADY_USED", "이미 사용된 이메일 인증입니다. 다시 인증해 주세요."),
    CANNOT_INVITE_SELF(HttpStatus.BAD_REQUEST, "CANNOT_INVITE_SELF", "자기 자신은 초대할 수 없습니다."),

    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "요청을 수행할 권한이 없습니다."),
    ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ROOM_ACCESS_DENIED", "참여 중인 방만 이용할 수 있습니다."),
    ROOM_OWNER_REQUIRED(HttpStatus.FORBIDDEN, "ROOM_OWNER_REQUIRED", "방장만 수행할 수 있습니다."),
    INVITATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "INVITATION_ACCESS_DENIED", "본인에게 온 초대만 처리할 수 있습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    INVITEE_NOT_FOUND(HttpStatus.NOT_FOUND, "INVITEE_NOT_FOUND", "초대할 회원을 찾을 수 없습니다."),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "방을 찾을 수 없습니다."),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "INVITATION_NOT_FOUND", "초대를 찾을 수 없습니다."),

    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "DUPLICATE_LOGIN_ID", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 가입된 이메일입니다."),
    ALREADY_ROOM_MEMBER(HttpStatus.CONFLICT, "ALREADY_ROOM_MEMBER", "이미 방에 참여 중인 회원입니다."),
    INVITATION_ALREADY_PENDING(HttpStatus.CONFLICT, "INVITATION_ALREADY_PENDING", "이미 대기 중인 초대가 있습니다."),
    INVITATION_ALREADY_RESPONDED(HttpStatus.CONFLICT, "INVITATION_ALREADY_RESPONDED", "이미 처리된 초대입니다."),
    ROOM_ALREADY_CLOSED(HttpStatus.CONFLICT, "ROOM_ALREADY_CLOSED", "이미 마감된 방입니다."),
    ROOM_NOT_CLOSED(HttpStatus.CONFLICT, "ROOM_NOT_CLOSED", "방이 마감된 후 결과를 확인할 수 있습니다."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "DATA_CONFLICT", "중복되거나 충돌하는 데이터입니다."),

    EMAIL_VERIFICATION_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "EMAIL_VERIFICATION_RESEND_TOO_SOON", "인증번호는 60초 후 다시 요청할 수 있습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
