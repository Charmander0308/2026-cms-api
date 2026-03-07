package com.malgn.configure.security;

import com.malgn.domain.user.entity.User;
import com.malgn.domain.user.entity.Role;
import com.malgn.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService 단위 테스트")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("존재하는 사용자명으로 조회하면 ADMIN 권한을 가진 UserDetails를 반환한다")
    void loadUserByUsername_ADMIN_사용자_성공() {
        // given
        User admin = User.builder()
                .username("admin")
                .password("encodedPassword")
                .role(Role.ADMIN)
                .build();
        given(userRepository.findByUsername("admin")).willReturn(Optional.of(admin));

        // when
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin");

        // then
        assertThat(userDetails.getUsername()).isEqualTo("admin");
        assertThat(userDetails.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("존재하는 사용자명으로 조회하면 USER 권한을 가진 UserDetails를 반환한다")
    void loadUserByUsername_USER_사용자_성공() {
        // given
        User user = User.builder()
                .username("user1")
                .password("encodedPassword")
                .role(Role.USER)
                .build();
        given(userRepository.findByUsername("user1")).willReturn(Optional.of(user));

        // when
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("user1");

        // then
        assertThat(userDetails.getUsername()).isEqualTo("user1");
        assertThat(userDetails.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }

    @Test
    @DisplayName("존재하지 않는 사용자명으로 조회하면 UsernameNotFoundException을 던진다")
    void loadUserByUsername_존재하지_않는_사용자() {
        // given
        given(userRepository.findByUsername("unknown")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}
