package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class KetQuaQuayResponse {
    private String ketQua; // "trung" | "truot"
    private KhuyenMaiResponse khuyenMai;             // null nếu trượt
    private PhieuGiamGiaCaNhanResponse phieuGiamGia; // null nếu trượt
    private Integer diemConLai;
}
