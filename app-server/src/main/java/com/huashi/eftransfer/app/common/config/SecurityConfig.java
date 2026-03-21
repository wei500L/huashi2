package com.huashi.eftransfer.app.common.config;

import com.huashi.eftransfer.app.common.security.JwtAuthenticationFilter;
import com.huashi.eftransfer.app.common.security.InternalApiAuthenticationFilter;
import com.huashi.eftransfer.app.common.security.handler.RestAccessDeniedHandler;
import com.huashi.eftransfer.app.common.security.handler.RestAuthenticationEntryPoint;
import com.huashi.eftransfer.app.common.security.store.AuthTokenStore;
import com.huashi.eftransfer.app.common.security.store.RedisAuthTokenStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, InternalApiProperties.class, CorsProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain appSecurityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            InternalApiAuthenticationFilter internalApiAuthenticationFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler,
            Environment environment
    )
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/api/health",
                            "/api/auth/login",
                            "/api/auth/refresh",
                            "/internal/**",
                            "/actuator/health",
                            "/actuator/info"
                    ).permitAll();
                    if (hasActiveProfile(environment, "local")) {
                        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll();
                    } else {
                        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").hasRole("ADMIN");
                    }
                    auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
                    auth.requestMatchers("/api/ai/**").hasAnyRole("STUDENT", "ADMIN");
                    auth.requestMatchers("/api/diagnosis/**").hasAnyRole("STUDENT", "ADMIN");
                    auth.requestMatchers("/api/training/**").hasAnyRole("STUDENT", "ADMIN");
                    auth.requestMatchers("/api/teacher/**").hasAnyRole("TEACHER", "ADMIN");
                    auth.requestMatchers("/api/student/**").hasAnyRole("STUDENT", "ADMIN");
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(internalApiAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, BasicAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setExposedHeaders(corsProperties.getExposedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge().getSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public RestAuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new RestAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public RestAccessDeniedHandler restAccessDeniedHandler(ObjectMapper objectMapper) {
        return new RestAccessDeniedHandler(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(AuthTokenStore.class)
    public AuthTokenStore authTokenStore(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        return new RedisAuthTokenStore(stringRedisTemplate, objectMapper);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private boolean hasActiveProfile(Environment environment, String profile) {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch(profile::equals);
    }
}
