package com.yourname.shorten.service;

import com.yourname.shorten.infrastructure.Base62Encoder;
import com.yourname.shorten.repository.UrlMap;
import com.yourname.shorten.repository.UrlMapRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortenService {

    private final UrlMapRepository repository;

    public ShortenService(UrlMapRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String shorten(String longUrl) {
        UrlMap entity = new UrlMap();
        entity.setLongUrl(longUrl);
        UrlMap saved = repository.save(entity);
        String shortCode = Base62Encoder.encode(saved.getId());
        saved.setShortCode(shortCode);
        repository.save(saved);
        return shortCode;
    }
}
