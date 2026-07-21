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
public class DmDoiThuongResponse {
    private Integer doiThuongId;
    private String ten;
    private String moTa;
    private Integer diemCan;
    private String loai;
    private BigDecimal giaTri;
    private BigDecimal giaTriToiDa;
    private String trangThai;
    private LocalDateTime ngayTao;
}
