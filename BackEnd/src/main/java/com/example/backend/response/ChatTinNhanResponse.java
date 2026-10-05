package com.example.backend.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatTinNhanResponse {
    private Long id;
    private Long cuocTroChuyenId;
    private String nguoiGui;
    private String loaiNguoiGui;
    private String tenNguoiGui;
    private String noiDung;
    private Boolean daDoc;
    private Boolean laCauHoiCuaAi;
    private LocalDateTime createdAt;
    private String trangThai;
}
