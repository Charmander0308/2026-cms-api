package com.malgn.domain.contents.controller;

import com.malgn.configure.security.JwtTokenProvider;
import com.malgn.domain.contents.dto.ContentsCreateRequest;
import com.malgn.domain.contents.dto.ContentsUpdateRequest;
import com.malgn.domain.contents.entity.Contents;
import com.malgn.domain.contents.repository.ContentsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DisplayName("ContentsController 통합 테스트")
class ContentsControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ContentsRepository contentsRepository;

    private String adminToken;
    private String userToken;

    // 각 테스트에서 공통으로 사용하는 테스트 데이터 ID
    private Long adminContentsId;
    private Long user1ContentsId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // 테스트 전용 JWT 토큰 생성 (DataInitializer가 admin/user1을 이미 DB에 삽입)
        adminToken = jwtTokenProvider.generateToken("admin", "ROLE_ADMIN");
        userToken  = jwtTokenProvider.generateToken("user1", "ROLE_USER");

        // 테스트 전용 콘텐츠 저장
        Contents adminContents = contentsRepository.save(Contents.builder()
                .title("Admin Test Contents")
                .description("Admin test description")
                .viewCount(0L)
                .createdDate(LocalDateTime.now())
                .createdBy("admin")
                .build());
        adminContentsId = adminContents.getId();

        Contents user1Contents = contentsRepository.save(Contents.builder()
                .title("User1 Test Contents")
                .description("User1 test description")
                .viewCount(0L)
                .createdDate(LocalDateTime.now())
                .createdBy("user1")
                .build());
        user1ContentsId = user1Contents.getId();
    }

    @AfterEach
    void tearDown() {
        // @SQLRestriction 으로 인해 소프트 삭제된 레코드는 existsById/deleteById 에서 필터링되므로
        // 소프트 삭제 여부와 무관하게 물리 삭제하는 전용 쿼리를 사용한다
        contentsRepository.hardDeleteById(adminContentsId);
        contentsRepository.hardDeleteById(user1ContentsId);
    }

    // ===================== 인증 검사 =====================

    @Test
    @DisplayName("토큰 없이 목록 조회하면 401을 반환한다")
    void getContentsList_토큰없음_401() throws Exception {
        // given: 인증 헤더 없음

        // when & then
        mockMvc.perform(get("/api/contents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 요청하면 401을 반환한다")
    void getContentsList_유효하지않은_토큰_401() throws Exception {
        // given
        String invalidToken = "invalid.jwt.token";

        // when & then
        mockMvc.perform(get("/api/contents")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    // ===================== 콘텐츠 목록 조회 =====================

    @Test
    @DisplayName("인증된 사용자가 목록을 조회하면 200과 페이징 응답을 반환한다")
    void getContentsList_성공() throws Exception {
        // given: adminToken (setUp에서 생성)

        // when & then
        mockMvc.perform(get("/api/contents")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @DisplayName("page·size 파라미터를 지정하면 해당 조건으로 페이징된 결과를 반환한다")
    void getContentsList_페이징_파라미터() throws Exception {
        // given
        int page = 0;
        int size = 1;

        // when & then
        mockMvc.perform(get("/api/contents")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(size))
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    // ===================== 콘텐츠 상세 조회 =====================

    @Test
    @DisplayName("존재하는 ID로 상세 조회하면 200과 콘텐츠 정보를 반환하고 조회수가 증가한다")
    void getContentsDetail_성공() throws Exception {
        // given: adminContentsId (setUp에서 생성, viewCount=0)

        // when & then
        mockMvc.perform(get("/api/contents/{id}", adminContentsId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(adminContentsId))
                .andExpect(jsonPath("$.data.title").value("Admin Test Contents"))
                .andExpect(jsonPath("$.data.viewCount").value(1)); // 조회수 +1
    }

    @Test
    @DisplayName("존재하지 않는 ID로 상세 조회하면 404를 반환한다")
    void getContentsDetail_존재하지_않는_ID_404() throws Exception {
        // given
        long nonExistentId = 999999L;

        // when & then
        mockMvc.perform(get("/api/contents/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("콘텐츠를 찾을 수 없습니다."));
    }

    // ===================== 콘텐츠 생성 =====================

    @Test
    @DisplayName("인증된 사용자가 유효한 요청으로 콘텐츠를 생성하면 201과 생성된 정보를 반환한다")
    void createContents_성공() throws Exception {
        // given
        ContentsCreateRequest request = new ContentsCreateRequest();
        request.setTitle("New Test Contents");
        request.setDescription("New test description");

        // when & then
        mockMvc.perform(post("/api/contents")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("New Test Contents"))
                .andExpect(jsonPath("$.data.createdBy").value("admin"))
                .andExpect(jsonPath("$.data.viewCount").value(0));
    }

    @Test
    @DisplayName("제목이 빈 값이면 400을 반환한다")
    void createContents_제목_빈값_400() throws Exception {
        // given
        ContentsCreateRequest request = new ContentsCreateRequest();
        request.setTitle("");
        request.setDescription("내용");

        // when & then
        mockMvc.perform(post("/api/contents")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("제목이 100자를 초과하면 400을 반환한다")
    void createContents_제목_100자_초과_400() throws Exception {
        // given
        ContentsCreateRequest request = new ContentsCreateRequest();
        request.setTitle("a".repeat(101));
        request.setDescription("내용");

        // when & then
        mockMvc.perform(post("/api/contents")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ===================== 콘텐츠 수정 =====================

    @Test
    @DisplayName("작성자 본인이 콘텐츠를 수정하면 200과 수정된 정보를 반환한다")
    void updateContents_작성자_본인_성공() throws Exception {
        // given: user1ContentsId는 user1이 작성한 콘텐츠
        ContentsUpdateRequest request = new ContentsUpdateRequest();
        request.setTitle("Updated Title by User1");
        request.setDescription("Updated description");

        // when & then
        mockMvc.perform(put("/api/contents/{id}", user1ContentsId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Title by User1"))
                .andExpect(jsonPath("$.data.lastModifiedBy").value("user1"))
                .andExpect(jsonPath("$.data.lastModifiedDate").isNotEmpty());
    }

    @Test
    @DisplayName("ADMIN이 타인의 콘텐츠를 수정하면 200을 반환한다")
    void updateContents_ADMIN_타인_콘텐츠_성공() throws Exception {
        // given: user1ContentsId는 user1 작성, adminToken으로 수정 시도
        ContentsUpdateRequest request = new ContentsUpdateRequest();
        request.setTitle("Modified by Admin");
        request.setDescription("Admin modified");

        // when & then
        mockMvc.perform(put("/api/contents/{id}", user1ContentsId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastModifiedBy").value("admin"));
    }

    @Test
    @DisplayName("작성자도 ADMIN도 아닌 사용자가 수정하면 403을 반환한다")
    void updateContents_권한없는_사용자_403() throws Exception {
        // given: adminContentsId는 admin 작성, userToken(user1)으로 수정 시도
        ContentsUpdateRequest request = new ContentsUpdateRequest();
        request.setTitle("Unauthorized Update");

        // when & then
        mockMvc.perform(put("/api/contents/{id}", adminContentsId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("수정 시 제목이 빈 값이면 400을 반환한다")
    void updateContents_제목_빈값_400() throws Exception {
        // given
        ContentsUpdateRequest request = new ContentsUpdateRequest();
        request.setTitle("");

        // when & then
        mockMvc.perform(put("/api/contents/{id}", adminContentsId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠를 수정하면 404를 반환한다")
    void updateContents_존재하지_않는_콘텐츠_404() throws Exception {
        // given
        ContentsUpdateRequest request = new ContentsUpdateRequest();
        request.setTitle("Title");

        // when & then
        mockMvc.perform(put("/api/contents/{id}", 999999L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ===================== 콘텐츠 삭제 =====================

    @Test
    @DisplayName("작성자 본인이 삭제하면 200을 반환한다")
    void deleteContents_작성자_본인_성공() throws Exception {
        // given: user1ContentsId는 user1 작성
        // tearDown에서 deleteById를 호출하지만 이미 삭제된 경우 existsById로 확인

        // when & then
        mockMvc.perform(delete("/api/contents/{id}", user1ContentsId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("ADMIN이 타인의 콘텐츠를 삭제하면 200을 반환한다")
    void deleteContents_ADMIN_타인_콘텐츠_성공() throws Exception {
        // given: user1ContentsId는 user1 작성, adminToken으로 삭제 시도

        // when & then
        mockMvc.perform(delete("/api/contents/{id}", user1ContentsId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("작성자도 ADMIN도 아닌 사용자가 삭제하면 403을 반환한다")
    void deleteContents_권한없는_사용자_403() throws Exception {
        // given: adminContentsId는 admin 작성, userToken(user1)으로 삭제 시도

        // when & then
        mockMvc.perform(delete("/api/contents/{id}", adminContentsId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠를 삭제하면 404를 반환한다")
    void deleteContents_존재하지_않는_콘텐츠_404() throws Exception {
        // given
        long nonExistentId = 999999L;

        // when & then
        mockMvc.perform(delete("/api/contents/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
