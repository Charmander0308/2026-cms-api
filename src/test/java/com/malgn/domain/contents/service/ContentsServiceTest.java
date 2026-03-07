package com.malgn.domain.contents.service;

import com.malgn.common.exception.BusinessException;
import com.malgn.common.exception.ErrorCode;
import com.malgn.domain.contents.dto.ContentsCreateRequest;
import com.malgn.domain.contents.dto.ContentsResponse;
import com.malgn.domain.contents.dto.ContentsUpdateRequest;
import com.malgn.domain.contents.entity.Contents;
import com.malgn.domain.contents.repository.ContentsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentsService 단위 테스트")
class ContentsServiceTest {

    @Mock
    private ContentsRepository contentsRepository;

    @InjectMocks
    private ContentsService contentsService;

    // ===================== createContents =====================

    @Test
    @DisplayName("유효한 요청으로 콘텐츠를 생성하면 저장된 콘텐츠 정보를 반환한다")
    void createContents_성공() {
        // given
        ContentsCreateRequest request = new ContentsCreateRequest();
        request.setTitle("새 콘텐츠");
        request.setDescription("콘텐츠 내용");

        Contents saved = Contents.builder()
                .id(1L)
                .title("새 콘텐츠")
                .description("콘텐츠 내용")
                .viewCount(0L)
                .createdDate(LocalDateTime.now())
                .createdBy("admin")
                .build();
        given(contentsRepository.save(any(Contents.class))).willReturn(saved);

        // when
        ContentsResponse response = contentsService.createContents(request, "admin");

        // then
        assertThat(response.getTitle()).isEqualTo("새 콘텐츠");
        assertThat(response.getDescription()).isEqualTo("콘텐츠 내용");
        assertThat(response.getCreatedBy()).isEqualTo("admin");
        assertThat(response.getViewCount()).isZero();
    }

    @Test
    @DisplayName("설명 없이 콘텐츠를 생성해도 성공한다")
    void createContents_설명_없이_성공() {
        // given
        ContentsCreateRequest request = new ContentsCreateRequest();
        request.setTitle("제목만 있는 콘텐츠");

        Contents saved = Contents.builder()
                .id(1L)
                .title("제목만 있는 콘텐츠")
                .description(null)
                .viewCount(0L)
                .createdDate(LocalDateTime.now())
                .createdBy("user1")
                .build();
        given(contentsRepository.save(any(Contents.class))).willReturn(saved);

        // when
        ContentsResponse response = contentsService.createContents(request, "user1");

        // then
        assertThat(response.getTitle()).isEqualTo("제목만 있는 콘텐츠");
        assertThat(response.getDescription()).isNull();
    }

    // ===================== getContentsList =====================

    @Test
    @DisplayName("페이징 조건으로 콘텐츠 목록을 조회하면 Page 형태로 반환한다")
    void getContentsList_성공() {
        // given
        Contents c1 = Contents.builder().id(1L).title("첫 번째").viewCount(0L).createdBy("admin").createdDate(LocalDateTime.now()).build();
        Contents c2 = Contents.builder().id(2L).title("두 번째").viewCount(5L).createdBy("user1").createdDate(LocalDateTime.now()).build();
        PageRequest pageable = PageRequest.of(0, 10);
        given(contentsRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(c1, c2)));

        // when
        Page<ContentsResponse> result = contentsService.getContentsList(pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(ContentsResponse::getTitle)
                .containsExactly("첫 번째", "두 번째");
    }

    @Test
    @DisplayName("콘텐츠가 없으면 빈 페이지를 반환한다")
    void getContentsList_빈_목록() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);
        given(contentsRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of()));

        // when
        Page<ContentsResponse> result = contentsService.getContentsList(pageable);

        // then
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    // ===================== getContentsDetail =====================

    @Test
    @DisplayName("존재하는 ID로 상세 조회하면 조회수가 1 증가하고 콘텐츠 정보를 반환한다")
    void getContentsDetail_성공_조회수_증가() {
        // given
        Contents contents = Contents.builder()
                .id(1L)
                .title("공지사항")
                .viewCount(10L)
                .createdBy("admin")
                .createdDate(LocalDateTime.now())
                .build();
        given(contentsRepository.findById(1L)).willReturn(Optional.of(contents));

        // when
        ContentsResponse response = contentsService.getContentsDetail(1L);

        // then
        assertThat(response.getViewCount()).isEqualTo(11L);
        then(contentsRepository).should().incrementViewCount(1L);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 상세 조회하면 BusinessException을 던진다")
    void getContentsDetail_존재하지_않는_ID() {
        // given
        given(contentsRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contentsService.getContentsDetail(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CONTENTS_NOT_FOUND.getMessage());
    }

    // ===================== updateContents =====================

    @Test
    @DisplayName("작성자 본인이 콘텐츠를 수정하면 제목·내용·수정자 정보가 갱신된다")
    void updateContents_작성자_본인_성공() {
        // given
        Contents contents = Contents.builder()
                .id(1L)
                .title("원래 제목")
                .description("원래 내용")
                .viewCount(0L)
                .createdBy("user1")
                .createdDate(LocalDateTime.now())
                .build();
        given(contentsRepository.findById(1L)).willReturn(Optional.of(contents));

        ContentsUpdateRequest request = new ContentsUpdateRequest();
        request.setTitle("수정된 제목");
        request.setDescription("수정된 내용");

        // when
        ContentsResponse response = contentsService.updateContents(1L, request, "user1", "ROLE_USER");

        // then
        assertThat(response.getTitle()).isEqualTo("수정된 제목");
        assertThat(response.getDescription()).isEqualTo("수정된 내용");
        assertThat(response.getLastModifiedBy()).isEqualTo("user1");
        assertThat(response.getLastModifiedDate()).isNotNull();
    }

    @Test
    @DisplayName("ADMIN은 타인의 콘텐츠도 수정할 수 있다")
    void updateContents_ADMIN_타인_콘텐츠_성공() {
        // given
        Contents contents = Contents.builder()
                .id(2L)
                .title("user1의 글")
                .description("user1이 작성")
                .viewCount(0L)
                .createdBy("user1")
                .createdDate(LocalDateTime.now())
                .build();
        given(contentsRepository.findById(2L)).willReturn(Optional.of(contents));

        ContentsUpdateRequest request = new ContentsUpdateRequest();
        request.setTitle("관리자가 수정");
        request.setDescription("관리자가 수정한 내용");

        // when
        ContentsResponse response = contentsService.updateContents(2L, request, "admin", "ROLE_ADMIN");

        // then
        assertThat(response.getTitle()).isEqualTo("관리자가 수정");
        assertThat(response.getLastModifiedBy()).isEqualTo("admin");
    }

    @Test
    @DisplayName("작성자도 ADMIN도 아닌 사용자가 수정하면 BusinessException을 던진다")
    void updateContents_권한_없는_사용자() {
        // given
        Contents contents = Contents.builder()
                .id(1L)
                .title("admin의 글")
                .viewCount(0L)
                .createdBy("admin")
                .createdDate(LocalDateTime.now())
                .build();
        given(contentsRepository.findById(1L)).willReturn(Optional.of(contents));

        ContentsUpdateRequest request = new ContentsUpdateRequest();
        request.setTitle("무단 수정 시도");

        // when & then
        assertThatThrownBy(() -> contentsService.updateContents(1L, request, "user1", "ROLE_USER"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("수정 대상 콘텐츠가 없으면 BusinessException을 던진다")
    void updateContents_존재하지_않는_콘텐츠() {
        // given
        given(contentsRepository.findById(999L)).willReturn(Optional.empty());
        ContentsUpdateRequest request = new ContentsUpdateRequest();
        request.setTitle("수정");

        // when & then
        assertThatThrownBy(() -> contentsService.updateContents(999L, request, "admin", "ROLE_ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CONTENTS_NOT_FOUND.getMessage());
    }

    // ===================== deleteContents =====================

    @Test
    @DisplayName("작성자 본인이 삭제하면 deletedAt이 설정된다")
    void deleteContents_작성자_본인_성공() {
        // given
        Contents contents = Contents.builder()
                .id(1L)
                .title("삭제할 글")
                .viewCount(0L)
                .createdBy("user1")
                .createdDate(LocalDateTime.now())
                .build();
        given(contentsRepository.findById(1L)).willReturn(Optional.of(contents));

        // when
        contentsService.deleteContents(1L, "user1", "ROLE_USER");

        // then — JPA dirty checking이 flush 처리하므로 deletedAt 설정 여부만 검증
        assertThat(contents.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("ADMIN은 타인의 콘텐츠도 삭제할 수 있다")
    void deleteContents_ADMIN_타인_콘텐츠_성공() {
        // given
        Contents contents = Contents.builder()
                .id(2L)
                .title("user1의 글")
                .viewCount(0L)
                .createdBy("user1")
                .createdDate(LocalDateTime.now())
                .build();
        given(contentsRepository.findById(2L)).willReturn(Optional.of(contents));

        // when
        contentsService.deleteContents(2L, "admin", "ROLE_ADMIN");

        // then — JPA dirty checking이 flush 처리하므로 deletedAt 설정 여부만 검증
        assertThat(contents.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("작성자도 ADMIN도 아닌 사용자가 삭제하면 BusinessException을 던지고 save는 호출되지 않는다")
    void deleteContents_권한_없는_사용자() {
        // given
        Contents contents = Contents.builder()
                .id(1L)
                .title("admin의 글")
                .viewCount(0L)
                .createdBy("admin")
                .createdDate(LocalDateTime.now())
                .build();
        given(contentsRepository.findById(1L)).willReturn(Optional.of(contents));

        // when & then
        assertThatThrownBy(() -> contentsService.deleteContents(1L, "user1", "ROLE_USER"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());

        then(contentsRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("삭제 대상 콘텐츠가 없으면 BusinessException을 던진다")
    void deleteContents_존재하지_않는_콘텐츠() {
        // given
        given(contentsRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contentsService.deleteContents(999L, "admin", "ROLE_ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CONTENTS_NOT_FOUND.getMessage());
    }
}
