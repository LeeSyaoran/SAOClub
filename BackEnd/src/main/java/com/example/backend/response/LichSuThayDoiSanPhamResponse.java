package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LichSuThayDoiSanPhamResponse {
    private Integer lichSuId;
    private String doiTuong;
    private Integer bienTheId;
    private String maSku;
    private String tenTruong;
    private String giaTriCu;
    private String giaTriMoi;
    private String tenNhanVien;
    private LocalDateTime thoiGian;
}
