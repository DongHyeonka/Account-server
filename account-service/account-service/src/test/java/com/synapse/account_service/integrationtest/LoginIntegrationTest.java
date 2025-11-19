package com.synapse.account_service.integrationtest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.account_service.TestConfig;
import com.synapse.account_service.domain.entity.Member;
import com.synapse.account_service.domain.enums.MemberRole;
import com.synapse.account_service.domain.repository.MemberRepository;
import com.synapse.account_service.exception.ExceptionType;
import com.synapse.account_service_api.dto.request.LoginRequest;

public class LoginIntegrationTest extends TestConfig{

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String TEST_USERNAME = "test_user6435";
    private final String TEST_PASSWORD = "password1234!";

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        Member testMember = Member.builder()
                .id(UUID.randomUUID())
                .email("test_user1234@example.com")
                .username(TEST_USERNAME)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .role(MemberRole.USER)
                .provider("local")
                .defaultWorkspaceId(UUID.randomUUID())
                .build();
        testMember = memberRepository.save(testMember);
    }

    @Test
    @DisplayName("로그인 성공: 올바른 아이디와 비밀번호로 요청 시, AccessToken과 RefreshToken 쿠키를 응답한다")
    void login_success() throws Exception {
        // given
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        // when
        ResultActions actions = mockMvc.perform(post("/api/accounts/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        actions
                .andDo(print()) // 요청/응답 전체 내용 출력
                .andExpect(status().isOk()) // 200 OK 상태 코드 확인
                .andExpect(cookie().exists("refreshToken")) // refreshToken 쿠키 존재 여부 확인
                .andExpect(cookie().httpOnly("refreshToken", true)) // HttpOnly 속성 확인
                .andExpect(jsonPath("$.accessToken").isNotEmpty()) // accessToken이 비어있지 않은지 확인
                .andExpect(jsonPath("$.expiresAt").isNumber()); // expiresAt이 숫자인지 확인
    }

    @Test
    @DisplayName("로그인 실패: 잘못된 비밀번호로 요청 시, 401 Unauthorized 상태 코드를 응답한다")
    void login_fail_with_wrong_password() throws Exception {
        // given
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, "wrong_password");
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        // when
        ResultActions actions = mockMvc.perform(post("/api/accounts/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        actions
                .andDo(print())
                .andExpect(status().isUnauthorized()) // 401 Unauthorized 상태 코드 확인
                .andExpect(jsonPath("$.code").value(ExceptionType.FAIL_LOGIN.getCode())); // LoginFailureHandler에서 정의한 에러 코드 확인
    }

    @Test
    @DisplayName("로그인 실패: 존재하지 않는 아이디로 요청 시, 401 Unauthorized 상태 코드를 응답한다")
    void login_fail_with_non_existent_username() throws Exception {
        // given
        LoginRequest loginRequest = new LoginRequest("non_existent_username", TEST_PASSWORD);
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        // when
        ResultActions actions = mockMvc.perform(post("/api/accounts/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        actions
                .andDo(print())
                .andExpect(status().isUnauthorized()) // 401 Unauthorized 상태 코드 확인
                .andExpect(jsonPath("$.code").value(ExceptionType.FAIL_LOGIN.getCode()));
    }
}
