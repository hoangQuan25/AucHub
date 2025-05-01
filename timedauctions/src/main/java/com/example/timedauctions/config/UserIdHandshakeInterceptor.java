package com.example.timedauctions.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

public class UserIdHandshakeInterceptor implements HandshakeInterceptor {

    private static final String WS_ATTR = "wsUserId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler handler,
                                   Map<String, Object> attrs) {

        if (request instanceof ServletServerHttpRequest servletReq) {
            String userId = UriComponentsBuilder
                    .fromUri(servletReq.getURI())
                    .build()
                    .getQueryParams()
                    .getFirst("uid");            // matches ?uid=… in the URL

            if (userId != null && !userId.isBlank()) {
                attrs.put(WS_ATTR, userId);   // ← stash it under "wsUserId"
            }
        }
        return true;  // continue with the handshake
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler handler,
                               Exception exception) {
        // no-op
    }
}
