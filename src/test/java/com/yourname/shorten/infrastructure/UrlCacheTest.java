package com.yourname.shorten.infrastructure;

import com.yourname.shorten.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UrlCacheTest extends AbstractIntegrationTest {

    @Autowired
    private UrlCache cache;

    @Test
    void putAndGet_returnsValue() {
        cache.put("abc", "https://example.com");
        assertThat(cache.get("abc")).contains("https://example.com");
    }

    @Test
    void get_miss_returnsEmpty() {
        assertThat(cache.get("missing")).isEmpty();
    }

    @Test
    void putNull_marksAsKnownMissing() {
        cache.putNull("ghost");
        assertThat(cache.isKnownMissing("ghost")).isTrue();
        assertThat(cache.get("ghost")).isEmpty();
    }
}
