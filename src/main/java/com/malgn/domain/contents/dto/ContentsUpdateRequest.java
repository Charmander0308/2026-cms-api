package com.malgn.domain.contents.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "콘텐츠 수정 요청")
@Getter
@Setter
@NoArgsConstructor
public class ContentsUpdateRequest {

    @Schema(description = "수정할 제목 (최대 100자)", example = "수정된 공지사항")
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이내여야 합니다.")
    private String title;

    @Schema(description = "수정할 내용 (선택)", example = "수정된 공지사항 내용입니다.")
    private String description;
}
