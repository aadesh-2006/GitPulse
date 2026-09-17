package com.gitpulse.domain.contributorfile;

import com.gitpulse.common.exception.AppException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RepositoryContributorFileSortValidator {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "filePath",
            "totalRevisions",
            "totalAdditions",
            "totalDeletions",
            "totalChurn",
            "firstContributedAt",
            "lastContributedAt",
            "contributorId"
    );

    private RepositoryContributorFileSortValidator() {
    }

    public static Pageable validateAndSanitize(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "totalChurn"));
        }

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "totalChurn"));
        }

        List<Sort.Order> sanitizedOrders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new AppException("Invalid sort field: '" + order.getProperty() + "'. Allowed sort fields are: " + ALLOWED_SORT_FIELDS);
            }
            String property = "contributorId".equals(order.getProperty()) ? "contributor.id" : order.getProperty();
            sanitizedOrders.add(new Sort.Order(order.getDirection(), property));
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(sanitizedOrders));
    }
}
