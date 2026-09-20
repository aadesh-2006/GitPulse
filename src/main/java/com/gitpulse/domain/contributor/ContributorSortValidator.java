package com.gitpulse.domain.contributor;

import com.gitpulse.common.exception.AppException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ContributorSortValidator {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "totalCommits",
            "totalAdditions",
            "totalDeletions",
            "totalChanges",
            "firstCommittedAt",
            "lastCommittedAt",
            "id",
            "contributorId"
    );

    private ContributorSortValidator() {
    }

    public static Pageable validateAndSanitize(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "totalCommits"));
        }

        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageSize, Sort.by(Sort.Direction.DESC, "totalCommits"));
        }

        List<Sort.Order> sanitizedOrders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new AppException("Invalid sort field: '" + order.getProperty() + "'. Allowed sort fields are: " + ALLOWED_SORT_FIELDS);
            }
            String property = "contributorId".equals(order.getProperty()) ? "contributor.id" : order.getProperty();
            sanitizedOrders.add(new Sort.Order(order.getDirection(), property));
        }

        return PageRequest.of(pageable.getPageNumber(), pageSize, Sort.by(sanitizedOrders));
    }
}
