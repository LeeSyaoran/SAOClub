package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kết quả của POST /api/san-pham.
 * Không trả thẳng entity SanPham nữa: entity có thuongHieu/danhMuc là @ManyToOne(LAZY)
 * được gán bằng getReferenceById() nên chỉ là proxy — Jackson vỡ khi ghi body, frontend
 * không đọc được id dù bản ghi đã lưu xong.
 */
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