package com.malgn.domain.contents.repository;

import com.malgn.domain.contents.entity.Contents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentsRepository extends JpaRepository<Contents, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Contents c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM Contents c WHERE c.id = :id")
    void hardDeleteById(@Param("id") Long id);
}
