package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Dành riêng cho tra cứu công khai theo SĐT (checkout) — CHỈ chứa thông tin cần để tự điền
// form, không bao giờ thêm soDuVi/diemTichLuy/trangThai vào đây vì endpoint này permitAll,
// ai cũng gọi được (kể cả dò số điện thoại ngẫu nhiên).
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
