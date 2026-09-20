package com.gitpulse.domain.commit;

import com.gitpulse.common.exception.AppException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class CommitSortValidator {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "committedAt",
            "additions",
            "deletions",
            "totalChanges",
            "githubCommitSha",
            "id"
    );

    private CommitSortValidator() {
    }

    public static Pageable validateAndSanitize(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "committedAt"));
        }

        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageSize, Sort.by(Sort.Direction.DESC, "committedAt"));
        }

        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new AppException("Invalid sort field: '" + order.getProperty() + "'. Allowed sort fields are: " + ALLOWED_SORT_FIELDS);
            }
        }

        return PageRequest.of(pageable.getPageNumber(), pageSize, pageable.getSort());
    }
}
