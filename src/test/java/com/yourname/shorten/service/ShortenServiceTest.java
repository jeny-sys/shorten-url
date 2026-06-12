package com.yourname.shorten.service;

import com.yourname.shorten.repository.UrlMap;
import com.yourname.shorten.repository.UrlMapRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortenServiceTest {

    @Mock
    private UrlMapRepository repository;

    @InjectMocks
    private ShortenService service;

    @Test
    void shorten_returnsBase62EncodedId() {
        UrlMap saved = new UrlMap();
        saved.setId(12345L);
        saved.setLongUrl("https://example.com/long");
        when(repository.save(any(UrlMap.class))).thenReturn(saved);

        String shortCode = service.shorten("https://example.com/long");

        assertThat(shortCode).isEqualTo("3d7");
        verify(repository, times(2)).save(any(UrlMap.class));
    }
}
