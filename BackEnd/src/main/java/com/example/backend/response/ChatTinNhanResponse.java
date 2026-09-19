package com.example.backend.response;

import lombok.*;

import java.time.LocalDateTime;

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
}
