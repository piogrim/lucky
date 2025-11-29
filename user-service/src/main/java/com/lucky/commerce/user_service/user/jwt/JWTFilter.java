package com.lucky.commerce.user_service.user.jwt;

import com.lucky.commerce.user_service.user.domain.Member;
import com.lucky.commerce.user_service.user.dto.CustomUserDetails;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    public JWTFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if(authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);

            return ;
        }

        String token = authorization.split(" ")[1];
        Claims claims = jwtUtil.getClaims(token);

        if(claims.getExpiration().before(new Date())){
            filterChain.doFilter(request, response);
            return ;
        }

        String username = claims.get("username",String.class);
        String role = claims.get("role",String.class);

        Member member = new Member();
        member.setUsername(username);
        member.setPassword("dummy");
        member.setRole(role);

        CustomUserDetails customUserDetails = new CustomUserDetails(member);

        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}