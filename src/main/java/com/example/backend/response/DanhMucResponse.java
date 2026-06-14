package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DanhMucResponse {
    private Integer id;
    private String tenDanhMuc;
    private String moTa;
    private String trangThai;
    private LocalDateTime ngayTao;
}
