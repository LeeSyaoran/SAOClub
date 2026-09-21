package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Phản hồi sau khi tạo mới sản phẩm
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SanPhamCreatedResponse {
    private Integer sanPhamId;
    private String maSanPham;
    private String barcode;
    private Integer bienTheId;
    private String maSku;
}