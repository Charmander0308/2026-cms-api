package com.malgn.domain.auth.service;

import com.malgn.configure.security.JwtTokenProvider;
import com.malgn.domain.auth.dto.LoginRequest;
import com.malgn.domain.auth.dto.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("올바른 자격증명으로 로그인하면 Bearer 타입의 JWT 토큰을 반환한다")
    void login_ADMIN_성공() {
        // given
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(jwtTokenProvider.generateToken("admin", "ROLE_ADMIN")).willReturn("mocked-jwt-token");

        // when
        TokenResponse response = authService.login(request);

        // then
        assertThat(response.getToken()).isEqualTo("mocked-jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("USER 역할 사용자도 올바른 자격증명으로 로그인하면 JWT 토큰을 반환한다")
    void login_USER_성공() {
        // given
        LoginRequest request = new LoginRequest();
        request.setUsername("user1");
        request.setPassword("user123");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user1", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(jwtTokenProvider.generateToken("user1", "ROLE_USER")).willReturn("user-jwt-token");

        // when
        TokenResponse response = authService.login(request);

        // then
        assertThat(response.getToken()).isEqualTo("user-jwt-token");
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인하면 BadCredentialsException을 던진다")
    void login_잘못된_비밀번호() {
        // given
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrongpassword");
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("자격 증명에 실패하였습니다."));

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("존재하지 않는 사용자명으로 로그인하면 BadCredentialsException을 던진다")
    void login_존재하지_않는_사용자() {
        // given
        LoginRequest request = new LoginRequest();
        request.setUsername("nouser");
        request.setPassword("password");
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("자격 증명에 실패하였습니다."));

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
