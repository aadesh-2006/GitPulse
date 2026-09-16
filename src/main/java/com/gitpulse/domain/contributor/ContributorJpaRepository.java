package com.gitpulse.domain.contributor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContributorJpaRepository extends JpaRepository<Contributor, Long> {

    Optional<Contributor> findByEmail(String email);

    List<Contributor> findByEmailIn(Collection<String> emails);

    boolean existsByEmail(String email);
}
