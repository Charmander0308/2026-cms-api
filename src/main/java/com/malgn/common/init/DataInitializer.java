package com.malgn.common.init;

import com.malgn.domain.contents.entity.Contents;
import com.malgn.domain.contents.repository.ContentsRepository;
import com.malgn.domain.user.entity.User;
import com.malgn.domain.user.entity.Role;
import com.malgn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ContentsRepository contentsRepository;
    private final PasswordEncoder passwordEncoder;

    // 애플리케이션 최초 기동 시 기본 사용자(admin, user1)와 샘플 콘텐츠를 DB에 삽입한다
    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .createdDate(LocalDateTime.now())
                .build());

        User user1 = userRepository.save(User.builder()
                .username("user1")
                .password(passwordEncoder.encode("user123"))
                .role(Role.USER)
                .createdDate(LocalDateTime.now())
                .build());

        contentsRepository.save(Contents.builder()
                .title("공지사항: 서비스 오픈 안내")
                .description("안녕하세요. 맑은기술 CMS 서비스가 오픈되었습니다. 많은 이용 부탁드립니다.")
                .viewCount(0L)
                .createdDate(LocalDateTime.now())
                .createdBy(admin.getUsername())
                .build());

        contentsRepository.save(Contents.builder()
                .title("첫 번째 게시글")
                .description("user1이 작성한 첫 번째 게시글입니다.")
                .viewCount(0L)
                .createdDate(LocalDateTime.now())
                .createdBy(user1.getUsername())
                .build());
    }
}
