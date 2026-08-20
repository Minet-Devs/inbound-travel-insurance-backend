package com.travel.insurance.config;

import com.travel.insurance.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling ->
                        handling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/v1/auth/**", "/error", "/api/v1/ussd/**", "/ussd/**").permitAll();
                    auth.requestMatchers("/api/v1/webhooks/biometric-verification").permitAll();
                    if (!environment.acceptsProfiles(Profiles.of("prod"))) {
                        auth.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    }
                    auth.requestMatchers("/api/v1/users/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/insurers").denyAll();
                    auth.requestMatchers("/api/v1/insurers/**").hasAnyRole("ADMIN", "INSURER_USER");
                    // Disable POST create endpoint specifically — must come BEFORE the broader service-providers rule
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/service-providers").denyAll();
                    auth.requestMatchers("/api/v1/service-providers/**")
                            .hasAnyRole("ADMIN", "PROVIDER_USER");
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/icd11-codes/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/departments/**", "/api/v1/medical-services/**",
                                    "/api/v1/organizations/**")
                            .hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.PUT, "/api/v1/departments/**", "/api/v1/medical-services/**",
                                    "/api/v1/organizations/**")
                            .hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.DELETE, "/api/v1/departments/**", "/api/v1/medical-services/**",
                                    "/api/v1/organizations/**")
                            .hasRole("ADMIN");
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
