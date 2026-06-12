package com.yourname.shorten.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ShortenRequest {

    @NotBlank
    @Size(max = 2048)
    private String url;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
