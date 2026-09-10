package com.helishop.core.config;

import com.helishop.core.security.CustomAccessDeniedHandler;
import com.helishop.core.security.CustomEntryPoint;
import com.helishop.core.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomEntryPoint customEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Endpoints công khai
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**").permitAll()

                        // 2. Phân quyền Người bán (SELLER) & Quản trị viên (ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasAnyRole("SELLER", "ADMIN")

                        // 3. Phân quyền Khách hàng (CUSTOMER) checkout đặt hàng
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/checkout").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/orders/**").authenticated()

                        // 4. Phân quyền Giỏ hàng
                        .requestMatchers("/api/v1/cart/**").authenticated()

                        // 5. Các yêu cầu còn lại bắt buộc đăng nhập
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
