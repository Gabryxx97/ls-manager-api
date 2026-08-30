package it.lsimpresa.manager.auth;

import lombok.Data;

public @Data class JwtTokenPair {
    private String accessToken;
    private String refreshToken;

    public JwtTokenPair(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
