package com.hxg.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub Pull Request 事件 DTO
 * 
 * 映射GitHub Webhook负载的DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PullRequestEventDTO(
        String action,
        @JsonProperty("pull_request") PullRequestDTO pullRequest,
        RepositoryDTO repository
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequestDTO(
            Integer number,
            String state,
            String title,
            @JsonProperty("diff_url") String diffUrl,
            @JsonProperty("html_url") String htmlUrl,
            UserDTO user,
            HeadDTO head,
            BaseDTO base
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserDTO(
            String login,
            @JsonProperty("avatar_url") String avatarUrl
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HeadDTO(
            String ref,
            String sha,
            RepositoryDTO repo
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BaseDTO(
            String ref,
            String sha,
            RepositoryDTO repo
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RepositoryDTO(
            Long id,
            String name,
            @JsonProperty("full_name") String fullName,
            @JsonProperty("html_url") String htmlUrl,
            UserDTO owner,
            @JsonProperty("clone_url") String cloneUrl
    ) {}
} 