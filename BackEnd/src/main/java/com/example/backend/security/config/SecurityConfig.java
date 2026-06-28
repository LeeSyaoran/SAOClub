package com.example.backend.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

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

    // ── Security filter chain ────────────────────────────────────────────────
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Tắt CSRF (REST API dùng JSON, không dùng form browser)
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless: không tạo session HTTP
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Phân quyền endpoint
            .authorizeHttpRequests(s -> s
                // Public: đăng nhập, đăng ký
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/khach-hang/register").permitAll()

                // Public: xem sản phẩm, danh mục (khách chưa đăng nhập vẫn xem được)
                .requestMatchers("/api/san-pham/**").permitAll()
                .requestMatchers("/api/danh-muc/**").permitAll()
                .requestMatchers("/api/thuong-hieu/**").permitAll()

                // Còn lại: cho phép tất cả (frontend tự xử lý phân quyền giao diện)
                // Khi cần bật strict authorization thì thay bằng .authenticated()
                .anyRequest().permitAll()
            )

            // Tắt form login mặc định của Spring (dùng REST login endpoint riêng)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
