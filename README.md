# 모일때 백엔드

여러 사람이 메신저에서 가능한 시간을 하나씩 확인하지 않아도 되도록, 초대받은 방 안에서 각자의 가능 시간을 등록하고 마감 후 가장 많이 겹치는 시간을 계산하는 MVP 백엔드입니다.

기존 `semi-backend-project-workspace`의 기능별 패키지 구성과 공통 응답·예외 처리 방식을 참고하되, 데이터 접근은 MyBatis 대신 Spring Data JPA로 구현했습니다.

## 핵심 정책

- 이메일 인증을 완료한 뒤 회원가입하고 로그인해야 모든 기능을 이용할 수 있습니다.
- 방 생성자는 자동으로 해당 방의 구성원이 됩니다.
- 방에는 초대를 수락한 회원만 들어갈 수 있습니다.
- 사용자는 자신이 참여 중인 방만 조회할 수 있습니다.
- 마감 전에는 본인의 가능 시간만 조회할 수 있고 집계 결과는 공개하지 않습니다.
- 가능 시간은 날짜별 시작·종료 구간을 여러 개 저장할 수 있으며, 저장할 때 기존 입력을 전체 교체합니다.
- 설정한 마감시각이 지나면 자동 마감되고 방장은 그 전에도 수동 마감할 수 있습니다.
- 마감 후에는 수정할 수 없으며, 가장 많은 회원이 가능한 시간대를 모두 반환합니다.

## 프로젝트 구조

```text
src/main/java/com/moiltae
├─ auth            # 로그인과 JWT 발급
├─ member          # 회원가입과 내 정보
├─ room            # 방, 방 구성원, 마감 스케줄러
├─ invitation      # 초대 생성·조회·수락·거절
├─ availability    # 가능 시간과 최다 겹침 계산
├─ health          # 상태 확인
└─ global          # 공통 응답, 예외, 보안, 시간 설정
```

각 기능은 `controller → service → repository → entity/dto` 방향으로 구성했습니다.

## 데이터베이스

| 테이블 | 주요 컬럼 | 역할 |
|---|---|---|
| `members` | `member_id`, `login_id`, `email`, `password`, `name` | 인증된 이메일을 가진 회원과 암호화된 비밀번호 |
| `email_verifications` | `email`, `code_hash`, `code_expires_at`, `verification_token`, `token_expires_at`, `used_at` | 이메일 인증번호와 일회용 가입 토큰 |
| `rooms` | `room_id`, `owner_id`, `title`, `start_date`, `end_date`, `closes_at`, `status`, `close_type`, `closed_at`, `created_at` | 일정 조율 방과 마감 상태 |
| `room_members` | `room_id`, `member_id`, `joined_at` | 초대를 수락해 방에 참여한 회원 |
| `invitations` | `invitation_id`, `room_id`, `invitee_id`, `status`, `requested_at`, `responded_at` | 초대 대기·수락·거절 상태 |
| `availabilities` | `availability_id`, `room_id`, `member_id`, `available_at`, `end_at` | 회원별 가능 시간 구간 |

`room_members`는 `(room_id, member_id)`, `availabilities`는 `(room_id, member_id, available_at)` 중복을 데이터베이스 제약조건으로 막습니다.

개발 환경의 `DDL_AUTO=update`에서는 JPA가 엔티티를 기준으로 테이블을 생성·갱신합니다. DBeaver에서 테이블을 직접 선언할 필요는 없으며, 접속 후 생성 결과를 조회하면 됩니다. 운영 전환 시에는 마이그레이션 도구를 도입하고 `DDL_AUTO=validate`로 바꾸는 것을 권장합니다.

- `local` 프로필(기본): 별도 설치 없이 프로젝트의 `data/moiltae`에 H2 파일 DB를 사용합니다.
- `mysql` 프로필: Docker Compose와 배포 환경에서 MySQL을 사용합니다.
- `mail` 프로필: `application-mail.yml`에서 SMTP 연결과 이메일 인증 유효시간을 관리하며 기본으로 포함됩니다.

## API

로그인과 회원가입을 제외한 요청에는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

| 기능 | Method | URL |
|---|---|---|
| 회원가입 | `POST` | `/api/v1/auth/signup` |
| 로그인 | `POST` | `/api/v1/auth/login` |
| 내 정보 | `GET` | `/api/v1/members/me` |
| 내 방 목록 | `GET` | `/api/v1/rooms` |
| 방 생성 | `POST` | `/api/v1/rooms` |
| 방 상세 | `GET` | `/api/v1/rooms/{roomId}` |
| 방 수동 마감 | `PATCH` | `/api/v1/rooms/{roomId}/close` |
| 받은 초대 | `GET` | `/api/v1/invitations?status=PENDING` |
| 회원 초대 | `POST` | `/api/v1/rooms/{roomId}/invitations` |
| 초대 수락·거절 | `PATCH` | `/api/v1/invitations/{invitationId}` |
| 내 가능 시간 | `GET` | `/api/v1/rooms/{roomId}/availabilities/me` |
| 내 가능 시간 저장 | `PUT` | `/api/v1/rooms/{roomId}/availabilities/me` |
| 마감 결과 | `GET` | `/api/v1/rooms/{roomId}/overlaps` |
| 서버 상태 | `GET` | `/api/v1/health` |

주요 요청 본문은 다음과 같습니다.

```json
// 회원가입
{
  "loginId": "student01",
  "password": "password123!",
  "name": "홍길동"
}
```

```json
// 방 생성
{
  "title": "프로젝트 회의 시간",
  "startDate": "2026-08-05",
  "endDate": "2026-08-06",
  "closesAt": "2026-08-05T18:00:00"
}
```

```json
// 초대 / 초대 응답
{ "loginId": "student02" }
{ "status": "ACCEPTED" }
```

초대 거절은 `REJECTED`를 사용합니다.

```json
// 가능 시간 전체 교체 저장
{
  "slots": [
    "2026-08-05T10:00:00",
    "2026-08-05T11:00:00"
  ]
}
```

모든 응답은 아래 공통 구조를 사용합니다.

```json
{
  "success": true,
  "code": "ROOM_CREATED",
  "message": "방을 생성했습니다.",
  "data": {},
  "errors": [],
  "timestamp": "2026-08-04T14:00:00"
}
```

## 실행

### Docker Compose

1. `.env.example`을 `.env`로 복사하고 비밀번호와 JWT 비밀키를 변경합니다.
2. 프로젝트 루트에서 실행합니다.

```bash
docker compose up --build
```

- API: `http://localhost:8889`
- 상태 확인: `http://localhost:8889/api/v1/health`
- Actuator 상태: `http://localhost:8889/actuator/health`
- MySQL: `localhost:${DB_PORT}` (기본 `3306`)

DBeaver에서는 호스트 `localhost`, 데이터베이스 `moiltae`, 사용자 `moiltae`, `.env`의 `DB_PASSWORD`로 접속할 수 있습니다.

### 로컬 실행

Java 21만 있으면 됩니다. 기본 `local` 프로필이 파일형 H2 데이터베이스를 자동 생성하므로 MySQL이 설치되어 있지 않아도 바로 실행됩니다.

```bash
./gradlew bootRun
```

Windows에서는 `gradlew.bat bootRun`을 사용합니다.

로컬 데이터는 프로젝트의 `data` 폴더에 유지됩니다. H2를 DBeaver로 직접 확인하려면 애플리케이션을 종료한 뒤 H2 드라이버와 `jdbc:h2:file:{프로젝트 절대경로}/data/moiltae;MODE=MySQL` 주소를 사용하고, 사용자는 `sa`, 비밀번호는 공란으로 접속합니다.

로컬에 설치한 MySQL을 직접 사용하려면 다음과 같이 프로필과 접속정보를 지정합니다.

```bash
SPRING_PROFILES_ACTIVE=mysql ./gradlew bootRun
```

Windows IDE에서는 실행 구성의 환경변수에 `SPRING_PROFILES_ACTIVE=mysql`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 등록하면 됩니다.

## 환경변수

| 이름 | 기본값 | 설명 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | `local`은 H2, `mysql`은 MySQL 사용 |
| `SERVER_PORT` | `8889` | 백엔드 HTTP 포트 |
| `LOCAL_DB_URL` | `jdbc:h2:file:./data/moiltae...` | local 프로필의 H2 주소 |
| `LOCAL_DB_USERNAME` | `sa` | local 프로필의 H2 사용자 |
| `LOCAL_DB_PASSWORD` | 공란 | local 프로필의 H2 비밀번호 |
| `DB_URL` | `jdbc:mysql://localhost:3306/moiltae...` | mysql 프로필의 JDBC 주소 |
| `DB_USERNAME` | `moiltae` | mysql 프로필의 DB 사용자 |
| `DB_PASSWORD` | 로컬 개발 기본값 | mysql 프로필의 DB 비밀번호 |
| `JWT_SECRET` | 로컬 개발 기본값 | Base64 인코딩된 32바이트 이상의 서명키 |
| `JWT_EXPIRATION_SECONDS` | `43200` | 토큰 유효시간(초) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | 허용할 프론트 주소, 쉼표로 복수 지정 |
| `DDL_AUTO` | `update` | JPA 스키마 정책 |
| `ROOM_CLOSE_INTERVAL_MS` | `60000` | 자동 마감 확인 간격(ms) |
| `MAIL_ENABLED` | `false` | `true`이면 SMTP로 인증 메일 발송, `false`이면 로컬 콘솔에 인증번호 출력 |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP 서버 주소 |
| `MAIL_PORT` | `587` | SMTP 포트 |
| `MAIL_USERNAME` | 공란 | SMTP 계정 |
| `MAIL_PASSWORD` | 공란 | SMTP 비밀번호 또는 앱 비밀번호 |
| `MAIL_FROM` | `MAIL_USERNAME` | 인증 메일 발신 주소 |
| `EMAIL_CODE_EXPIRATION_SECONDS` | `300` | 인증번호 유효시간 |
| `EMAIL_TOKEN_EXPIRATION_SECONDS` | `900` | 인증 완료 후 가입 토큰 유효시간 |
| `EMAIL_RESEND_COOLDOWN_SECONDS` | `60` | 인증번호 재발송 대기시간 |

배포 환경에서는 반드시 DB 비밀번호, JWT 비밀키, SMTP 계정을 별도 값으로 주입해야 합니다. 로컬에서 `MAIL_ENABLED=false`로 실행하면 실제 메일 대신 백엔드 콘솔의 `[LOCAL EMAIL VERIFICATION]` 로그에서 인증번호를 확인할 수 있습니다.

## 테스트

```bash
./gradlew test
```

통합 테스트는 H2의 MySQL 호환 모드에서 다음 흐름을 검증합니다.

- 로그인 성공·실패와 JWT 발급
- 초대 전 방 접근 차단
- 초대 생성과 수락 후 방 참여
- 마감 전 결과 비공개
- 가능 시간 등록과 공동 최다 시간대 계산
- 수동 마감과 마감 후 수정 차단
