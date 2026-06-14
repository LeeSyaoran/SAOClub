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
public class NhaCungCapResponse {
    private Integer nhaCungCapId;
    private String tenNhaCungCap;
    private String soDienThoai;
    private String email;
    private String diaChi;
    private String maSoThue;
    private String nguoiLienHe;
    private String trangThai;
    private LocalDateTime ngayTao;
}
