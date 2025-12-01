package com.lucky.commerce.user_service.user.domain;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@RedisHash(value = "refreshToken", timeToLive = 604800)
public class RefreshToken {

    @Id
    private String refreshToken;

    private String username;

    public RefreshToken(final String refreshToken, final String username) {
        this.refreshToken = refreshToken;
        this.username = username;
    }
}