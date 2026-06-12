package com.yourname.shorten.controller;

import com.yourname.shorten.AbstractIntegrationTest;
import com.yourname.shorten.repository.UrlMap;
import com.yourname.shorten.repository.UrlMapRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class RedirectIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UrlMapRepository repository;

    @Test
    void getRedirect_known_returns302() throws Exception {
        UrlMap entity = new UrlMap();
        entity.setShortCode("abc");
        entity.setLongUrl("https://example.com/target");
        repository.save(entity);

        mockMvc.perform(get("/abc"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/target"));
    }

    @Test
    void getRedirect_unknown_returns404() throws Exception {
        mockMvc.perform(get("/nope"))
                .andExpect(status().isNotFound());
    }
}
