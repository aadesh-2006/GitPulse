package com.gitpulse.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubFileResponse {

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("status")
    private String status;

    @JsonProperty("additions")
    private Integer additions;

    @JsonProperty("deletions")
    private Integer deletions;

    @JsonProperty("changes")
    private Integer changes;

    @JsonProperty("blob_url")
    private String blobUrl;

    @JsonProperty("raw_url")
    private String rawUrl;

    @JsonProperty("previous_filename")
    private String previousFilename;

    public GitHubFileResponse() {
    }

    public GitHubFileResponse(String filename,
                              String status,
                              Integer additions,
                              Integer deletions,
                              Integer changes,
                              String blobUrl,
                              String rawUrl,
                              String previousFilename) {
        this.filename = filename;
        this.status = status;
        this.additions = additions;
        this.deletions = deletions;
        this.changes = changes;
        this.blobUrl = blobUrl;
        this.rawUrl = rawUrl;
        this.previousFilename = previousFilename;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAdditions() {
        return additions;
    }

    public void setAdditions(Integer additions) {
        this.additions = additions;
    }

    public Integer getDeletions() {
        return deletions;
    }

    public void setDeletions(Integer deletions) {
        this.deletions = deletions;
    }

    public Integer getChanges() {
        return changes;
    }

    public void setChanges(Integer changes) {
        this.changes = changes;
    }

    public String getBlobUrl() {
        return blobUrl;
    }

    public void setBlobUrl(String blobUrl) {
        this.blobUrl = blobUrl;
    }

    public String getRawUrl() {
        return rawUrl;
    }

    public void setRawUrl(String rawUrl) {
        this.rawUrl = rawUrl;
    }

    public String getPreviousFilename() {
        return previousFilename;
    }

    public void setPreviousFilename(String previousFilename) {
        this.previousFilename = previousFilename;
    }
}
