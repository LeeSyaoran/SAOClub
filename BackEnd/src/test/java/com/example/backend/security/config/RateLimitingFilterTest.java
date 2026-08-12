package com.example.backend.security.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RateLimitingFilterTest {

    private final RateLimitingFilter filter = new RateLimitingFilter();

    @Test
    void timTheoSdt_qua5LanTrongMotPhut_chanLanThu6() throws Exception {
        for (int i = 1; i <= 5; i++) {
            FilterChain chain = mock(FilterChain.class);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(requestTimTheoSdt(), res, chain);
            verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        }

        FilterChain chain6 = mock(FilterChain.class);
        MockHttpServletResponse res6 = new MockHttpServletResponse();
        filter.doFilter(requestTimTheoSdt(), res6, chain6);

        assertThat(res6.getStatus()).isEqualTo(429);
        verifyNoInteractions(chain6);
    }

    private MockHttpServletRequest requestTimTheoSdt() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/khach-hang/tim-theo-sdt");
        req.setRemoteAddr("1.2.3.4");
        return req;
    }
}
