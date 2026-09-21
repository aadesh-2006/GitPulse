package com.gitpulse.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepositoryJpaRepository extends JpaRepository<Repository, Long> {

    boolean existsByOwnerAndName(String owner, String name);

    boolean existsByFullName(String fullName);

    Optional<Repository> findByOwnerAndName(String owner, String name);

    Optional<Repository> findByFullName(String fullName);

    Optional<Repository> findByFullNameIgnoreCase(String fullName);
}
