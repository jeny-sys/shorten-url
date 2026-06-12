package com.yourname.shorten.controller;

import com.yourname.shorten.service.RedirectService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Optional;

@RestController
public class RedirectController {

    private final RedirectService service;

    public RedirectController(RedirectService service) {
        this.service = service;
    }

    @GetMapping("/{shortCode:[a-zA-Z0-9]+}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        Optional<String> target = service.resolve(shortCode);
        if (target.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(target.get()));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
