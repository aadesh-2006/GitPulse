package com.gitpulse.domain.repository.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateRepositoryRequest {

    @NotBlank(message = "Owner must not be blank")
    @Size(max = 100, message = "Owner must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Owner must contain only alphanumeric characters, underscores, dots, or hyphens")
    private String owner;

    @NotBlank(message = "Repository name must not be blank")
    @Size(max = 100, message = "Repository name must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Repository name must contain only alphanumeric characters, underscores, dots, or hyphens")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 100, message = "Default branch must not exceed 100 characters")
    private String defaultBranch;

    public CreateRepositoryRequest() {
    }

    public CreateRepositoryRequest(String owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public CreateRepositoryRequest(String owner, String name, String description, String defaultBranch) {
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.defaultBranch = defaultBranch;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }
}
