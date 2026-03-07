package com.malgn.domain.contents.dto;

import com.malgn.domain.contents.entity.Contents;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ContentsResponse {

    private Long id;
    private String title;
    private String description;
    private Long viewCount;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime lastModifiedDate;
    private String lastModifiedBy;

    public static ContentsResponse from(Contents contents) {
        return ContentsResponse.builder()
                .id(contents.getId())
                .title(contents.getTitle())
                .description(contents.getDescription())
                .viewCount(contents.getViewCount())
                .createdDate(contents.getCreatedDate())
                .createdBy(contents.getCreatedBy())
                .lastModifiedDate(contents.getLastModifiedDate())
                .lastModifiedBy(contents.getLastModifiedBy())
                .build();
    }
}
