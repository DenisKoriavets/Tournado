package com.tournado.core.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtTokenService,
                jwtProperties,
                restAuthenticationEntryPoint
        );

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(restAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/health", "/api/v1/health").permitAll()
                        .requestMatchers("/api/v1/auth/steam/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/email/request-link", "/api/v1/auth/logout")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/email/verify").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tournaments/*/disputes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tournaments/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/teams/my").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/teams/*", "/api/v1/teams/invites/*").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
