package com.gitpulse.integration.github.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "github.api")
public class GitHubProperties {

    private String baseUrl = "https://api.github.com";
    private String token;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(10);
    private int commitPageSize = 30;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getCommitPageSize() {
        return commitPageSize;
    }

    public void setCommitPageSize(int commitPageSize) {
        if (commitPageSize < 1) {
            this.commitPageSize = 1;
        } else if (commitPageSize > 100) {
            this.commitPageSize = 100;
        } else {
            this.commitPageSize = commitPageSize;
        }
    }
}
