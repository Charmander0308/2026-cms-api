package com.malgn.domain.contents.service;

import com.malgn.common.exception.BusinessException;
import com.malgn.common.exception.ErrorCode;
import com.malgn.domain.contents.dto.ContentsCreateRequest;
import com.malgn.domain.contents.dto.ContentsResponse;
import com.malgn.domain.contents.dto.ContentsUpdateRequest;
import com.malgn.domain.contents.entity.Contents;
import com.malgn.domain.contents.repository.ContentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentsService {

    private final ContentsRepository contentsRepository;

    @Transactional
    public ContentsResponse createContents(ContentsCreateRequest request, String username) {
        Contents contents = Contents.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .viewCount(0L)
                .createdDate(LocalDateTime.now())
                .createdBy(username)
                .build();
        return ContentsResponse.from(contentsRepository.save(contents));
    }

    public Page<ContentsResponse> getContentsList(Pageable pageable) {
        return contentsRepository.findAll(pageable).map(ContentsResponse::from);
    }

    // 특정 콘텐츠를 조회하고 조회수를 1 증가시킨 뒤 결과를 반환한다
    @Transactional
    public ContentsResponse getContentsDetail(Long id) {
        Contents contents = findById(id);
        contentsRepository.incrementViewCount(id);
        // DB 업데이트 후 응답에 반영 (incrementViewCount로 DB는 +1됨)
        contents.setViewCount(contents.getViewCount() + 1);
        return ContentsResponse.from(contents);
    }

    @Transactional
    public ContentsResponse updateContents(Long id, ContentsUpdateRequest request, String username, String role) {
        Contents contents = findById(id);
        checkEditPermission(contents, username, role);

        contents.setTitle(request.getTitle());
        contents.setDescription(request.getDescription());
        contents.setLastModifiedDate(LocalDateTime.now());
        contents.setLastModifiedBy(username);

        return ContentsResponse.from(contents);
    }

    @Transactional
    public void deleteContents(Long id, String username, String role) {
        Contents contents = findById(id);
        checkEditPermission(contents, username, role);
        contents.setDeletedAt(LocalDateTime.now());
    }

    private Contents findById(Long id) {
        return contentsRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENTS_NOT_FOUND));
    }

    // 콘텐츠 수정·삭제 권한을 검사한다. ADMIN이거나 작성자 본인이 아니면 예외를 던진다
    private void checkEditPermission(Contents contents, String username, String role) {
        boolean isAdmin = "ROLE_ADMIN".equals(role);
        boolean isOwner = contents.getCreatedBy().equals(username);
        if (!isAdmin && !isOwner) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
