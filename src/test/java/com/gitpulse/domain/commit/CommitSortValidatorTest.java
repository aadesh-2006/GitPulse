package com.gitpulse.domain.commit;

import com.gitpulse.common.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommitSortValidatorTest {

    @Test
    @DisplayName("Null pageable defaults to page 0, size 20, committedAt DESC")
    void validateAndSanitize_NullPageable() {
        Pageable result = CommitSortValidator.validateAndSanitize(null);
        assertThat(result.getPageNumber()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(20);
        assertThat(result.getSort().getOrderFor("committedAt")).isNotNull();
        assertThat(result.getSort().getOrderFor("committedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("Unsorted pageable defaults to committedAt DESC preserving page and size")
    void validateAndSanitize_Unsorted() {
        Pageable result = CommitSortValidator.validateAndSanitize(PageRequest.of(2, 50));
        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(50);
        assertThat(result.getSort().getOrderFor("committedAt")).isNotNull();
        assertThat(result.getSort().getOrderFor("committedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("All allowed sort fields pass validation")
    void validateAndSanitize_AllowedFields() {
        String[] allowed = {"committedAt", "additions", "deletions", "totalChanges", "githubCommitSha", "id"};
        for (String field : allowed) {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, field));
            Pageable result = CommitSortValidator.validateAndSanitize(pageable);
            assertThat(result.getSort().getOrderFor(field)).isNotNull();
        }
    }

    @Test
    @DisplayName("Disallowed sort field throws AppException")
    void validateAndSanitize_DisallowedField() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "message"));
        assertThatThrownBy(() -> CommitSortValidator.validateAndSanitize(pageable))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid sort field: 'message'");
    }
}
