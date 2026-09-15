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
}
