package com.malgn.configure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

    private static final String SECRET_KEY = "ThisIsASecretKeyForJwtTokenSigningMustBe256BitsLongAtLeast!!";
    private static final long EXPIRATION = 86400000L;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtTokenProvider, "expiration", EXPIRATION);
    }

    @Test
    @DisplayName("사용자명과 역할을 담아 토큰을 생성하면 비어있지 않은 문자열을 반환한다")
    void generateToken_성공() {
        // given
        String username = "admin";
        String role = "ROLE_ADMIN";

        // when
        String token = jwtTokenProvider.generateToken(username, role);

        // then
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("유효한 토큰에서 사용자명을 정확히 추출한다")
    void getUsername_유효한_토큰() {
        // given
        String username = "admin";
        String token = jwtTokenProvider.generateToken(username, "ROLE_ADMIN");

        // when
        String extracted = jwtTokenProvider.getUsername(token);

        // then
        assertThat(extracted).isEqualTo(username);
    }

    @Test
    @DisplayName("서명과 만료기간이 유효한 토큰은 검증에 성공한다")
    void validateToken_유효한_토큰() {
        // given
        String token = jwtTokenProvider.generateToken("admin", "ROLE_ADMIN");

        // when
        boolean result = jwtTokenProvider.validateToken(token);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("올바르지 않은 형식의 토큰은 검증에 실패한다")
    void validateToken_잘못된_형식() {
        // given
        String invalidToken = "invalid.jwt.token";

        // when
        boolean result = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("빈 문자열 토큰은 검증에 실패한다")
    void validateToken_빈_문자열() {
        // given
        String emptyToken = "";

        // when
        boolean result = jwtTokenProvider.validateToken(emptyToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 검증에 실패한다")
    void validateToken_만료된_토큰() {
        // given: 만료 시간을 음수로 설정하여 이미 만료된 토큰 생성
        JwtTokenProvider expiredProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(expiredProvider, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(expiredProvider, "expiration", -1L);
        String expiredToken = expiredProvider.generateToken("admin", "ROLE_ADMIN");

        // when
        boolean result = jwtTokenProvider.validateToken(expiredToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("USER 역할로 생성한 토큰에서도 사용자명을 정확히 추출한다")
    void getUsername_USER_역할() {
        // given
        String username = "user1";
        String token = jwtTokenProvider.generateToken(username, "ROLE_USER");

        // when
        String extracted = jwtTokenProvider.getUsername(token);

        // then
        assertThat(extracted).isEqualTo(username);
    }
}
