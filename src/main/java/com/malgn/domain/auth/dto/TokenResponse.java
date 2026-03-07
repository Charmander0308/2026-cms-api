package com.malgn.domain.auth.dto;

import lombok.Getter;

@Getter
public class TokenResponse {

    private final String tokenType = "Bearer";
    private final String token;

    public TokenResponse(String token) {
        this.token = token;
    }
}
