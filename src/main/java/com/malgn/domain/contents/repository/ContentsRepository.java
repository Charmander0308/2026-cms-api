package com.malgn.domain.contents.repository;

import com.malgn.domain.contents.entity.Contents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ContentsRepository extends JpaRepository<Contents, Long> {

    @Query("SELECT c FROM Contents c WHERE c.createdBy IN (SELECT u.username FROM User u)")
    Page<Contents> findAllByActiveUsers(Pageable pageable);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Contents c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query("DELETE FROM Contents c WHERE c.id = :id")
    void hardDeleteById(@Param("id") Long id);
}
