package com.upsjb.ms1.security.config;

import com.upsjb.ms1.security.filter.RequestAuditFilter;
import com.upsjb.ms1.security.filter.RequestTraceFilter;
import com.upsjb.ms1.security.filter.SecurityContextFilter;
import com.upsjb.ms1.security.handler.RestAccessDeniedHandler;
import com.upsjb.ms1.security.handler.RestAuthenticationEntryPoint;
import com.upsjb.ms1.security.handler.RestLogoutSuccessHandler;
import com.upsjb.ms1.security.jwt.RoleJwtAuthenticationConverter;
import com.upsjb.ms1.security.oauth2.CustomOAuth2UserService;
import com.upsjb.ms1.security.oauth2.OAuth2LoginFailureHandler;
import com.upsjb.ms1.security.oauth2.OAuth2LoginSuccessHandler;
import com.upsjb.ms1.security.roles.SecurityRoles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/password/forgot",
            "/api/password/reset",
            "/api/verificaciones/enviar",
            "/api/verificaciones/validar",
            "/api/verificaciones/reenviar",
            "/oauth2/**",
            "/login/oauth2/**",
            "/.well-known/**",
            "/actuator/health",
            "/actuator/health/**",
            "/error"
    };

    private static final String[] DEVELOPMENT_DOC_ENDPOINTS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**"
    };

    private static final String[] ADMIN_ENDPOINTS = {
            "/api/usuarios/**",
            "/api/roles/**",
            "/api/auditoria/**"
    };

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            RestLogoutSuccessHandler logoutSuccessHandler,
            RoleJwtAuthenticationConverter roleJwtAuthenticationConverter,
            RequestTraceFilter requestTraceFilter,
            RequestAuditFilter requestAuditFilter,
            SecurityContextFilter securityContextFilter,
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
            OAuth2LoginFailureHandler oAuth2LoginFailureHandler
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(DEVELOPMENT_DOC_ENDPOINTS).permitAll()
                        .requestMatchers(ADMIN_ENDPOINTS).hasAuthority(SecurityRoles.AUTHORITY_ADMIN)
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/auth/logout").authenticated()
                        .requestMatchers("/api/auth/logout-all").authenticated()
                        .requestMatchers("/api/sesiones/**").authenticated()
                        .requestMatchers("/api/password/change/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(roleJwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                )
                .addFilterBefore(requestTraceFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(requestAuditFilter, RequestTraceFilter.class)
                .addFilterAfter(securityContextFilter, RequestAuditFilter.class);

        return http.build();
    }
}
