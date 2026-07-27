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
public class NhanVienResponse {
    private Integer nhanVienId;
    private String hoTen;
    private String soDienThoai;
    private String email;
    private Integer chucVuId;
    private String username;
    private BigDecimal luongCoBan;
    private String trangThai;
    private LocalDateTime ngayTao;
}
