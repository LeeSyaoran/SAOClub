package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class KhuyenMaiSanPhamResponse {
    private Integer khuyenMaiId;
    private List<SanPhamSimpleResponse> sanPhams;

    @lombok.Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanPhamSimpleResponse {
        private Integer sanPhamId;
        private String tenSanPham;
        private String hinhAnhChinh;
    }
}
