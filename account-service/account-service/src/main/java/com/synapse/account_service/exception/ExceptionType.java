package com.synapse.account_service.exception;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ExceptionType {
    // 1xx - 회원/가입
    DUPLICATED_EMAIL(CONFLICT, "101", "이미 존재하는 이메일입니다."),
    DUPLICATED_USERNAME(CONFLICT, "102", "이미 존재하는 사용자 이름입니다."),
    DUPLICATED_USERNAME_AND_EMAIL(CONFLICT, "103", "이미 존재하는 사용자 이름과 이메일입니다."),
    NOT_FOUND_MEMBER(NOT_FOUND, "104", "존재하지 않는 사용자입니다."),

    // 2xx - 인증/토큰
    INVALID_ACCESS_TOKEN(UNAUTHORIZED, "201", "유효하지 않은 액세스 토큰입니다."),
    EXPIRED_TOKEN(UNAUTHORIZED, "202", "만료된 토큰입니다."),
    INVALID_REFRESH_TOKEN(UNAUTHORIZED, "203", "유효하지 않은 리프레시 토큰입니다."),
    TAMPERED_REFRESH_TOKEN(UNAUTHORIZED, "204", "리프레시 토큰이 변조되었습니다."),
    FAIL_LOGIN(UNAUTHORIZED, "205", "아이디 또는 비밀번호가 일치하지 않습니다."),

    // 9xx - 시스템 공통
    EXCEPTION(INTERNAL_SERVER_ERROR, "900", "예상치 못한 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
