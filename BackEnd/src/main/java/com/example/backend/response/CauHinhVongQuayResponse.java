package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CauHinhVongQuayResponse {
    private Integer diemMoiLuot;
    private Integer tyLeTruot;
    private List<KhuyenMaiResponse> khuyenMaiKhaDung;
}
