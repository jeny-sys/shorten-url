package com.yourname.shorten.service;

import com.yourname.shorten.infrastructure.UrlCache;
import com.yourname.shorten.repository.UrlMapRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RedirectService {

    private final UrlMapRepository repository;
    private final UrlCache cache;

    public RedirectService(UrlMapRepository repository, UrlCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

    public Optional<String> resolve(String shortCode) {
        if (cache.isKnownMissing(shortCode)) {
            return Optional.empty();
        }
        Optional<String> cached = cache.get(shortCode);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<String> loaded = repository.findByShortCode(shortCode)
                .map(entity -> entity.getLongUrl());
        if (loaded.isPresent()) {
            cache.put(shortCode, loaded.get());
        } else {
            cache.putNull(shortCode);
        }
        return loaded;
    }
}
