package com.travel.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(Long userId, String username, Integer role) {
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withClaim("role", role)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expiration))
                .sign(Algorithm.HMAC256(secret));
    }

    public Long getUserIdFromToken(String token) {
        DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
        return jwt.getClaim("userId").asLong();
    }

    public String getUsernameFromToken(String token) {
        DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
        return jwt.getClaim("username").asString();
    }

    public Integer getRoleFromToken(String token) {
        DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
        return jwt.getClaim("role").asInt();
    }

    public boolean validateToken(String token) {
        try {
            JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
