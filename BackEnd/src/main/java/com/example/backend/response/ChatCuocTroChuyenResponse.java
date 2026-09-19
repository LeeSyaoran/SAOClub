package com.example.backend.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatCuocTroChuyenResponse {
    private Long id;
    private String loaiKhach;
    private Integer khachHangId;
    private String hoTenKhach;
    private String trangThai;
    private Integer nhanVienPhuTrachId;
    private String nhanVienPhuTrachTen;
    private Integer soLanEscalate;
    private Integer tinNhanChuaDoc;
    private String tinNhanCuoi;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
