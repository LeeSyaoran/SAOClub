package com.example.backend.response;

import lombok.*;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class DanhMucResponse {
    private Integer id;
    private String tenDanhMuc;
    private String moTa;
    private String trangThai;
    private LocalDateTime ngayTao;
}
