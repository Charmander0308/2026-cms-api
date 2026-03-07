package com.malgn.domain.contents.controller;

import com.malgn.common.response.ApiResponse;
import com.malgn.common.response.PageResponse;
import com.malgn.domain.contents.dto.ContentsCreateRequest;
import com.malgn.domain.contents.dto.ContentsResponse;
import com.malgn.domain.contents.dto.ContentsUpdateRequest;
import com.malgn.domain.contents.service.ContentsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Contents", description = "콘텐츠 관리")
@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentsController {

    private final ContentsService contentsService;

    @Operation(summary = "콘텐츠 생성", description = "새 콘텐츠를 생성합니다. 인증이 필요합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검사 실패", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ContentsResponse>> createContents(
            @Valid @RequestBody ContentsCreateRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(contentsService.createContents(request, username)));
    }

    @Operation(summary = "콘텐츠 목록 조회", description = "페이징 조건으로 콘텐츠 목록을 조회합니다. 기본 정렬: 최신순.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
    })
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0", in = ParameterIn.QUERY),
            @Parameter(name = "size", description = "페이지 크기", example = "10", in = ParameterIn.QUERY),
            @Parameter(name = "sort", description = "정렬 기준 (예: createdDate,desc)", example = "createdDate,desc", in = ParameterIn.QUERY)
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ContentsResponse>>> getContentsList(
            @Parameter(hidden = true) @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ContentsResponse> page = contentsService.getContentsList(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    @Operation(summary = "콘텐츠 상세 조회", description = "특정 콘텐츠를 조회합니다. 조회할 때마다 view_count가 1 증가합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "콘텐츠 없음", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentsResponse>> getContentsDetail(
            @Parameter(description = "콘텐츠 ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(contentsService.getContentsDetail(id)));
    }

    @Operation(summary = "콘텐츠 수정", description = "콘텐츠를 수정합니다. 본인이 작성한 콘텐츠 또는 ADMIN만 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검사 실패", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "콘텐츠 없음", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentsResponse>> updateContents(
            @Parameter(description = "콘텐츠 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ContentsUpdateRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        String username = authentication.getName();
        String role = extractRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(contentsService.updateContents(id, request, username, role)));
    }

    @Operation(summary = "콘텐츠 삭제", description = "콘텐츠를 논리 삭제합니다 (Soft Delete). 본인이 작성한 콘텐츠 또는 ADMIN만 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "콘텐츠 없음", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContents(
            @Parameter(description = "콘텐츠 ID", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        String username = authentication.getName();
        String role = extractRole(authentication);
        contentsService.deleteContents(id, username, role);
        return ResponseEntity.ok(ApiResponse.success());
    }

    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");
    }
}
