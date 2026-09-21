package com.gitpulse.benchmark.dto;

public interface FutureFileOutcomeRow {

    String getFilePath();

    long getFutureRevisionCount();

    long getFutureChurn();

    long getFutureDistinctContributors();
}
