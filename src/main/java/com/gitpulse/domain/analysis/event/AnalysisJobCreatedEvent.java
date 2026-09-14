package com.gitpulse.domain.analysis.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class AnalysisJobCreatedEvent {

    private final UUID eventId;
    private final Long analysisJobId;
    private final Long repositoryId;
    private final String repositoryFullName;
    private final Instant timestamp;
    private final String eventVersion;

    @JsonCreator
    public AnalysisJobCreatedEvent(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("analysisJobId") Long analysisJobId,
            @JsonProperty("repositoryId") Long repositoryId,
            @JsonProperty("repositoryFullName") String repositoryFullName,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("eventVersion") String eventVersion) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID();
        this.analysisJobId = Objects.requireNonNull(analysisJobId, "analysisJobId must not be null");
        this.repositoryId = Objects.requireNonNull(repositoryId, "repositoryId must not be null");
        this.repositoryFullName = repositoryFullName;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.eventVersion = eventVersion != null ? eventVersion : "1.0";
    }

    public static AnalysisJobCreatedEvent of(Long analysisJobId, Long repositoryId, String repositoryFullName) {
        return new AnalysisJobCreatedEvent(
                UUID.randomUUID(),
                analysisJobId,
                repositoryId,
                repositoryFullName,
                Instant.now(),
                "1.0"
        );
    }

    public UUID getEventId() {
        return eventId;
    }

    public Long getAnalysisJobId() {
        return analysisJobId;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getEventVersion() {
        return eventVersion;
    }

    @Override
    public String toString() {
        return "AnalysisJobCreatedEvent{" +
                "eventId=" + eventId +
                ", analysisJobId=" + analysisJobId +
                ", repositoryId=" + repositoryId +
                ", repositoryFullName='" + repositoryFullName + '\'' +
                ", timestamp=" + timestamp +
                ", eventVersion='" + eventVersion + '\'' +
                '}';
    }
}
