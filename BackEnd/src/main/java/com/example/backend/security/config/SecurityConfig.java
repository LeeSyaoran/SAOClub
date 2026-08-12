package com.example.backend.security.config;

import com.example.backend.security.jwt.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:4173",
                "http://192.168.0.101:5173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)

            .cors(c -> c.configurationSource(corsConfigurationSource()))

            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) ->
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Chưa đăng nhập hoặc phiên đã hết hạn"))
                .accessDeniedHandler((req, res, ex) ->
                    res.sendError(HttpServletResponse.SC_FORBIDDEN, "Không có quyền truy cập"))
            )

            .authorizeHttpRequests(s -> s
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/khach-hang/register").permitAll()
                .requestMatchers("/api/khach-hang/tim-theo-sdt").permitAll()
                .requestMatchers("/api/khach-hang/khach-vang-lai").permitAll()

                .requestMatchers(HttpMethod.GET, "/api/cai-dat").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/san-pham/**", "/api/danh-muc/**", "/api/thuong-hieu/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/dm-cpu/**", "/api/dm-ram/**", "/api/dm-gpu/**", "/api/dm-o-cung/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/danh-gia/**").permitAll()

                .anyRequest().authenticated()
            )

            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
