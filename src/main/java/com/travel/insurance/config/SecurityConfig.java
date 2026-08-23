package com.travel.insurance.config;

import com.travel.insurance.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling ->
                        handling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/v1/auth/**", "/error", "/api/v1/ussd/**", "/ussd/**").permitAll();
                    auth.requestMatchers("/api/v1/webhooks/biometric-verification").permitAll();
                    if (!environment.acceptsProfiles(Profiles.of("prod"))) {
                        auth.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    }
                    auth.requestMatchers(HttpMethod.POST,"/api/v1/users").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.GET,"/api/v1/users").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/insurers").denyAll();
                    auth.requestMatchers("/api/v1/insurers/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/policies").denyAll();
                    // Disable POST create endpoint specifically — must come BEFORE the broader service-providers rule
                    // insurers & service-providers are now create through organization to enable role based system access
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/service-providers").denyAll();
                    auth.requestMatchers("/api/v1/service-providers/**").hasAnyRole("ADMIN", "PROVIDER_USER");
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/icd11-codes/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/departments/**", "/api/v1/medical-services/**", "/api/v1/organizations/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.PUT, "/api/v1/departments/**", "/api/v1/medical-services/**", "/api/v1/organizations/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.DELETE, "/api/v1/departments/**", "/api/v1/medical-services/**", "/api/v1/organizations/**").hasRole("ADMIN");
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
