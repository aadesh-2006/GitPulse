package com.gitpulse.integration.github.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GitHubProperties.class)
public class GitHubClientConfig {

    @Bean
    public RestClient gitHubRestClient(GitHubProperties properties, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());

        RestClient.Builder customBuilder = builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("User-Agent", "GitPulse");

        if (properties.getToken() != null && !properties.getToken().isBlank()) {
            customBuilder.defaultHeader("Authorization", "Bearer " + properties.getToken().trim());
        }

        return customBuilder.build();
    }
}
