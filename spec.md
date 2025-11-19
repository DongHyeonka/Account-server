### 1. JWT 스펙 v1

1-1 토큰 종류

- User Access Token
    - 발급처 : Account-Server
    - 용도 : 유저 요청 인증 (클라 → API Gateway → 백엔드)
    - 서명 알고리즘 : HS256 (대칭키, ACCOUNT_JWT_SECRET)
    - 전달 경로 :
        - 클라이언트 → Authorization: Bearer <access-token>
        - Gateway에서 검증 후 아래 헤더로 전달
            - X-User-Id
            - X-Workspace-Id
            - X-Subscription-Tier
            - X-User-Role

- User Refresh Token
    - 발급처 : Account-Server
    - 용도 : Access Token 재발급
    - 서명 알고리즘 : HS256
    - 전달 경로 : HTTP only 쿠키 or 헤더

- Service Token
    - 발급처 : Authorization-Server
    - 용도 : 서비스 간 통신 (gateway → account /api/internal/**)
    - 그랜트 : client_credentials
    - 서명 알고리즘 : RS256 (비대칭키, Authorization-Server의 JWK)
    - Resource Server의 설정으로만 검증, Gateway는 이 토큰을 직접 검증 안함

1-2 공통 클레임 규칙

| Claim | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| iss | string | O | 토큰 발급자 (Account-Server) |
| aud | string or string[] | O | 토큰 대상 gateway, internal |
| sub | string | O | 주체 User 토큰이면 MemberId Service 토큰이면 ClientId |
| iat | number (epoch seconds) | O | 발급 시간 |
| exp | number (epoch seconds) | O | 만료 시간 |
| nbf | number | 선택 | 필요시 시용 |
| jti | string | 선택 | 토큰 ID에 대한 블랙리스트(필요시) |

1-3 User Access Token 스펙

| Claim | 타입 | 예시 | 설명 |
| --- | --- | --- | --- |
| iss | string | account-server | account-server 고정 |
| aud | string | api-gateway-server | API Gateway 전용 토큰 |
| sub | string | member_234234 | 내부 member 식별자 |
| iat | number | 1700000000 | 발급 시간 |
| exp | number | 1700003600 | 만료 시간 |
| workspace_id | string | “workspace_id” | 기본/현재 |
| tier | string | free / pro | 구독 등급 |
| role | string | User / admin | 애플리케이션 역항 |
| username | string | “kanf” | 화면 표시용 닉네임 or 로그인 ID |

1-4 User Refresh Token 스펙

| Claim | 타입 | 예시 | 설명 |
| --- | --- | --- | --- |
| iss | string | account-server | 동일 |
| aud | string | account-server | 재발급 |
| sub | string | member_741267848 | 유저 ID |
| iat | number | 1700000000 | 발급 시간 |
| exp | number | 1700003600 | 만료 시간 |

1-5 Service Token 스펙

| Cliam | 타입 | 예시 | 설명 |
| --- | --- | --- | --- |
| iss | string | account-server | 동일 |
| aud | string | account-server | 서비스 이름 |
| sub | string | client-id | Client ID |
| scope | string | "api.internal” | 권한 |
| iat | number | 1700000000 | 발급 시간 |
| exp | number | 1700000000 | 만료 시간 |

### 2. 에러 코드

| 코드 | 도메인 | 설명 | HTTP Status |
| --- | --- | --- | --- |
| 101 | 회원/가입 | 이미 존재하는 이메일입니다. | 409 |
| 102 | 회원/가입 | 이미 존재하는 사용자 이름입니다. | 409 |
| 103 | 회원/가입 | 이미 존재하는 사용자 이름과 이메일입니다. | 409 |
| 104 | 회원/가입 | 존재하지 않는 사용자입니다. | 404 |
| 201 | 인증/토큰 | 유효하지 않은 액세스 토큰입니다. | 401 |
| 202 | 인증/토큰 | 만료된 토큰입니다. | 401 |
| 203 | 인증/토큰 | 유효하지 않은 리프레시 토큰입니다. | 401 |
| 204 | 인증/토큰 | 리프레시 토큰이 변조되었습니다. | 401 |
| 205 | 인증/토큰 | 아이디 또는 비밀번호가 일치하지 않습니다. | 401 |
| 900 | 공통 | 예상치 못한 오류가 발생했습니다. | 500 |