package com.yourname.shorten.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UrlMapRepository extends JpaRepository<UrlMap, Long> {
    Optional<UrlMap> findByShortCode(String shortCode);
}
