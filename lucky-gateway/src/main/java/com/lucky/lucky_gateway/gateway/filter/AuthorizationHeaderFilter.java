package com.lucky.lucky_gateway.gateway.filter;

import com.lucky.lucky_gateway.gateway.jwt.GatewayJwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {

    private final GatewayJwtUtil jwtUtil;

    @Autowired
    public AuthorizationHeaderFilter(GatewayJwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    public static class Config {
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String authorizationHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            String jwt = authorizationHeader.replace("Bearer ", "");

            if (!jwtUtil.validateToken(jwt)) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String username = jwtUtil.getUsername(jwt);
            Long userId = jwtUtil.getUserId(jwt);

            ServerHttpRequest newRequest = request.mutate()
                    .header("X-User-Name", username)
                    .header("X-User-Id", userId.toString())
                    .build();

            return chain.filter(exchange.mutate().request(newRequest).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }
}
