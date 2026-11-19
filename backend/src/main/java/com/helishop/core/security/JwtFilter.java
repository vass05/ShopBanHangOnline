package com.helishop.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final com.helishop.core.modules.user.repository.UserRepository users;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUsernameFromJwtToken(jwt);
                Long userId = jwtUtils.getUserIdFromJwtToken(jwt);
                var user = userId == null ? null : users.findById(userId).orElse(null);
                if (user != null && user.getStatus() == com.helishop.core.common.constants.UserStatus.ACTIVE
                        && user.getEmail().equals(username) && user.getAuthVersion() == jwtUtils.getAuthVersionFromJwtToken(jwt)) {
                String role = user.getRole().name();

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (StringUtils.hasText(role)) {
                    // Chuẩn hóa role luôn có tiền tố ROLE_ để tương thích hasRole() và hasAuthority()
                    String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    authorities.add(new SimpleGrantedAuthority(normalizedRole));
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUser(jwtUtils.getUserIdFromJwtToken(jwt), username), null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            log.debug("Không thể thiết lập xác thực JWT: {}", e.getClass().getSimpleName());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
