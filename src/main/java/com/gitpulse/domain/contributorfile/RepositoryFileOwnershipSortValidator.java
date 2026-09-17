package com.gitpulse.domain.contributorfile;

import com.gitpulse.common.exception.AppException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RepositoryFileOwnershipSortValidator {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "filePath",
            "contributorCount",
            "totalRevisionsAcrossContributors",
            "topContributorRevisionShare",
            "topContributorId"
    );

    private RepositoryFileOwnershipSortValidator() {
    }

    public static Pageable validateAndSanitize(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(
                    Sort.Order.desc("topContributorRevisionShare"),
                    Sort.Order.asc("filePath")
            ));
        }

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(
                    Sort.Order.desc("topContributorRevisionShare"),
                    Sort.Order.asc("filePath")
            ));
        }

        List<Sort.Order> sanitizedOrders = new ArrayList<>();
        boolean containsFilePath = false;
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new AppException("Invalid sort field: '" + order.getProperty() + "'. Allowed sort fields are: " + ALLOWED_SORT_FIELDS);
            }
            sanitizedOrders.add(order);
            if ("filePath".equals(order.getProperty())) {
                containsFilePath = true;
            }
        }

        if (!containsFilePath) {
            sanitizedOrders.add(Sort.Order.asc("filePath"));
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(sanitizedOrders));
    }
}