package com.yourname.shorten.controller;

import com.yourname.shorten.controller.dto.ShortenRequest;
import com.yourname.shorten.controller.dto.ShortenResponse;
import com.yourname.shorten.service.ShortenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shorten")
public class ShortenController {

    private final ShortenService service;
    private final String publicBaseUrl;

    public ShortenController(ShortenService service,
                             @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.service = service;
        this.publicBaseUrl = publicBaseUrl;
    }

    @PostMapping
    public ShortenResponse shorten(@Valid @RequestBody ShortenRequest request) {
        String code = service.shorten(request.getUrl());
        return new ShortenResponse(code, publicBaseUrl + "/" + code);
    }
}
