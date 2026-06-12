package com.yourname.shorten.service;

import com.yourname.shorten.repository.UrlMapRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RedirectService {

    private final UrlMapRepository repository;

    public RedirectService(UrlMapRepository repository) {
        this.repository = repository;
    }

    public Optional<String> resolve(String shortCode) {
        return repository.findByShortCode(shortCode)
                .map(entity -> entity.getLongUrl());
    }
}
