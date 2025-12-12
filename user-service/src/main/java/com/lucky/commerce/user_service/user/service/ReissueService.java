package com.lucky.commerce.user_service.user.service;

import com.lucky.commerce.user_service.user.domain.RefreshTokenRepository;
import com.lucky.commerce.user_service.user.jwt.JWTUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ReissueService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTUtil jwtUtil;

    @Autowired
    public ReissueService(RefreshTokenRepository refreshTokenRepository, JWTUtil jwtUtil) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
    }

    public String reissue(String refreshToken) {

        Claims claims = jwtUtil.getClaims(refreshToken);

        if(claims == null){
            return null;
        }

        String username = claims.get("username", String.class);
        Long id = claims.get("userId", Long.class);

        if(claims.getExpiration().before(new Date())){
            return null;
        }

        if(!claims.get("category",String.class).equals("refresh")){
            return null;
        }

        boolean isExist = refreshTokenRepository.existsById(refreshToken);

        if(!isExist){
            return null;
        }

        return jwtUtil.createAccessToken(id, username,"ROLE_USER",600000L);
    }
}
