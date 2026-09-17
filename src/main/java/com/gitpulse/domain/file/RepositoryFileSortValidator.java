package com.gitpulse.domain.file;

import com.gitpulse.common.exception.AppException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class RepositoryFileSortValidator {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "filePath",
            "totalRevisions",
            "totalAdditions",
            "totalDeletions",
            "totalChurn",
            "firstModifiedAt",
            "lastModifiedAt"
    );

    private RepositoryFileSortValidator() {
    }

    public static Pageable validateAndSanitize(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "totalChurn"));
        }

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "totalChurn"));
        }

        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new AppException("Invalid sort field: '" + order.getProperty() + "'. Allowed sort fields are: " + ALLOWED_SORT_FIELDS);
            }
        }

        return pageable;
    }
}