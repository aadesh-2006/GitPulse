package com.gitpulse.domain.contributorfile.dto;

public interface RepositoryFileOwnershipRow {

    Long getRepositoryId();

    String getFilePath();

    long getContributorCount();

    long getTotalRevisionsAcrossContributors();

    Double getTopContributorRevisionShare();

    Long getTopContributorId();
}