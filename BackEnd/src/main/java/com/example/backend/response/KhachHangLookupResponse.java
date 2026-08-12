package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class KhachHangLookupResponse {
    private Integer khachHangId;
    private String hoTen;
    private String soDienThoai;
    private String email;
    private String diaChi;
}
