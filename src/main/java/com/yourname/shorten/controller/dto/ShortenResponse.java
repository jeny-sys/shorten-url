package com.yourname.shorten.controller.dto;

public class ShortenResponse {

    private final String shortCode;
    private final String shortUrl;

    public ShortenResponse(String shortCode, String shortUrl) {
        this.shortCode = shortCode;
        this.shortUrl = shortUrl;
    }

    public String getShortCode() { return shortCode; }
    public String getShortUrl() { return shortUrl; }
}
