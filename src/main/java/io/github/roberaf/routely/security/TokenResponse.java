package io.github.roberaf.routely.security;

public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
