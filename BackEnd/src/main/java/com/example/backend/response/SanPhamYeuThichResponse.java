package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SanPhamYeuThichResponse {
    private Integer yeuThichId;
    private Integer bienTheId;
    private Integer sanPhamId;
    private String tenSanPham;
    private String tenThuongHieu;
    private String maSku;
    private BigDecimal giaBan;
    private String hinhAnhChinh;
    private String trangThai;
    private Long soLuongTon;
    private LocalDateTime ngayThem;
}
