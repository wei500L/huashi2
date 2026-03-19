package com.huashi.eftransfer.app.common.security;

import com.huashi.eftransfer.app.common.security.store.AuthTokenStore;
import com.huashi.eftransfer.shared.api.ResultCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_CODE = "auth.error.code";
    public static final String AUTH_ERROR_MESSAGE = "auth.error.message";
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenStore authTokenStore;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AuthTokenStore authTokenStore) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authTokenStore = authTokenStore;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                JwtPrincipal principal = jwtTokenProvider.parseAccessToken(token);
                if (authTokenStore.isAccessTokenBlacklisted(principal.tokenId())) {
                    request.setAttribute(AUTH_ERROR_CODE, ResultCode.TOKEN_INVALID);
                    request.setAttribute(AUTH_ERROR_MESSAGE, "Access token has been revoked");
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                var authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        token,
                        principal.authorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException ex) {
                log.warn("event=jwt_expired reason={}", ex.getMessage());
                request.setAttribute(AUTH_ERROR_CODE, ResultCode.TOKEN_EXPIRED);
                request.setAttribute(AUTH_ERROR_MESSAGE, "Access token has expired");
                SecurityContextHolder.clearContext();
            } catch (JwtException ex) {
                log.warn("event=jwt_rejected reason={}", ex.getMessage());
                request.setAttribute(AUTH_ERROR_CODE, ResultCode.TOKEN_INVALID);
                request.setAttribute(AUTH_ERROR_MESSAGE, "Access token is invalid");
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
