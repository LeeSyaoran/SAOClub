package com.example.backend.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TonKhoResponse {
    private Integer tonKhoId;
    private Integer bienTheId;
    private String maSku;
    private String tenSanPham;
    private String mauSac;
    private Integer soLuongTon;
    private Integer soLuongGiu;
    private Integer tonKhoToiThieu;
    private LocalDateTime ngayCapNhat;
    private Long soLuongDaBan;
    private Long tongSerial;

    public TonKhoResponse(Integer tonKhoId, Integer bienTheId, String maSku,
                          String tenSanPham, String mauSac,
                          Integer soLuongTon, Integer soLuongGiu,
                          Integer tonKhoToiThieu, LocalDateTime ngayCapNhat) {
        this.tonKhoId = tonKhoId;
        this.bienTheId = bienTheId;
        this.maSku = maSku;
        this.tenSanPham = tenSanPham;
        this.mauSac = mauSac;
        this.soLuongTon = soLuongTon;
        this.soLuongGiu = soLuongGiu;
        this.tonKhoToiThieu = tonKhoToiThieu;
        this.ngayCapNhat = ngayCapNhat;
    }
}
