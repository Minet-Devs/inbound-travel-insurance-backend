package com.travel.insurance.auth;

import com.travel.insurance.common.util.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            jwtTokenProvider.parse(header.substring(BEARER_PREFIX.length()))
                    .filter(this::isAccessToken)
                    .ifPresent(claims -> authenticate(claims, request));
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAccessToken(Claims claims) {
        return JwtTokenProvider.TOKEN_TYPE_ACCESS.equals(claims.get(JwtTokenProvider.CLAIM_TOKEN_TYPE));
    }

    private void authenticate(Claims claims, HttpServletRequest request) {
        String role = claims.get(JwtTokenProvider.CLAIM_ROLE, String.class);
        String organizationId = claims.get(JwtTokenProvider.CLAIM_ORGANIZATION_ID, String.class);
        AuthenticatedUser principal = new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                organizationId != null ? UUID.fromString(organizationId) : null,
                role);
        List<SimpleGrantedAuthority> authorities = role == null || role.isBlank()
                ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + role));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
