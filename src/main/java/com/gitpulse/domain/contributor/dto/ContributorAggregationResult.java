package com.gitpulse.domain.contributor.dto;

public class ContributorAggregationResult {

    private final int contributorsAggregated;
    private final int contributorsCreated;
    private final int attributionsCreated;
    private final int attributionsUpdated;
    private final long durationMs;

    public ContributorAggregationResult(int contributorsAggregated,
                                        int contributorsCreated,
                                        int attributionsCreated,
                                        int attributionsUpdated,
                                        long durationMs) {
        this.contributorsAggregated = contributorsAggregated;
        this.contributorsCreated = contributorsCreated;
        this.attributionsCreated = attributionsCreated;
        this.attributionsUpdated = attributionsUpdated;
        this.durationMs = durationMs;
    }

    public int getContributorsAggregated() {
        return contributorsAggregated;
    }

    public int getContributorsCreated() {
        return contributorsCreated;
    }

    public int getAttributionsCreated() {
        return attributionsCreated;
    }

    public int getAttributionsUpdated() {
        return attributionsUpdated;
    }

    public long getDurationMs() {
        return durationMs;
    }

    @Override
    public String toString() {
        return "ContributorAggregationResult{" +
                "contributorsAggregated=" + contributorsAggregated +
                ", contributorsCreated=" + contributorsCreated +
                ", attributionsCreated=" + attributionsCreated +
                ", attributionsUpdated=" + attributionsUpdated +
                ", durationMs=" + durationMs +
                '}';
    }
}
