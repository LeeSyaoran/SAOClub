package com.example.backend.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@Order(2)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, RateLimitEntry> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (canBiGioiHan(request)) {
            String ip = getClientIP(request);
            RateLimitEntry entry = attempts.compute(ip, (key, val) -> {
                if (val == null || val.isExpired()) {
                    return new RateLimitEntry();
                }
                val.increment();
                return val;
            });

            if (entry.getCount() > 5) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Quá nhiều yêu cầu đăng nhập. Vui lòng thử lại sau 1 phút.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // Giới hạn cho các endpoint permitAll mà nếu spam được sẽ dò ra thông tin/vét cạn tài
    // khoản: đăng nhập (brute-force mật khẩu) và tra cứu SĐT (vét cạn số điện thoại để biết
    // ai đã là khách hàng, xem KhachHangController.findBySoDienThoai).
    private boolean canBiGioiHan(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if ("/api/auth/login".equals(uri) && "POST".equalsIgnoreCase(method)) return true;
        return "/api/khach-hang/tim-theo-sdt".equals(uri) && "GET".equalsIgnoreCase(method);
    }

    private String getClientIP(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private static class RateLimitEntry {
        private int count = 1;
        private final long windowStart = System.currentTimeMillis();

        void increment() { count++; }
        int getCount() { return count; }
        boolean isExpired() {
            return System.currentTimeMillis() - windowStart > TimeUnit.MINUTES.toMillis(1);
        }
    }
}
