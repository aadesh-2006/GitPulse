package com.gitpulse.benchmark.dto;

public interface HistoricalAuthorFileCountRow {

    String getFilePath();

    String getAuthorKey();

    long getRevisionCount();
}
