package com.example.backend.security.config;

import com.example.backend.security.jwt.JwtAuthFilter;
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

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    // ── BCrypt password encoder ──────────────────────────────────────────────
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── AuthenticationManager — dùng trong AuthController ───────────────────
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ── CORS — 1 chỗ duy nhất, thay cho @CrossOrigin lặp lại ở từng controller ──
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ── Security filter chain ────────────────────────────────────────────────
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Tắt CSRF (REST API dùng JSON, không dùng form browser)
            .csrf(AbstractHttpConfigurer::disable)

            .cors(c -> c.configurationSource(corsConfigurationSource()))

            // Stateless: không tạo session HTTP
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Phân quyền endpoint
            .authorizeHttpRequests(s -> s
                // Public: đăng nhập, đăng ký
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/khach-hang/register").permitAll()
                .requestMatchers("/api/khach-hang/tim-theo-sdt").permitAll()
                .requestMatchers("/api/khach-hang/khach-vang-lai").permitAll()

                // Public: xem sản phẩm, danh mục (khách chưa đăng nhập vẫn xem được)
                .requestMatchers("/api/san-pham/**").permitAll()
                .requestMatchers("/api/danh-muc/**").permitAll()
                .requestMatchers("/api/thuong-hieu/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/dm-cpu/**", "/api/dm-ram/**", "/api/dm-gpu/**", "/api/dm-o-cung/**").permitAll()

                // Còn lại: bắt buộc đăng nhập (JWT hợp lệ) — phân quyền chi tiết theo role
                // dùng @PreAuthorize ở từng controller quản trị (nhân viên, chức vụ...)
                .anyRequest().authenticated()
            )

            // Tắt form login mặc định của Spring (dùng REST login endpoint riêng)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
