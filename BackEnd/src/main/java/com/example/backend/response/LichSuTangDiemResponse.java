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
public class LichSuTangDiemResponse {
    private Integer id;
    private Integer soDiem;
    private String lyDo;
    private String tenNhanVien;
    private LocalDateTime ngayTao;
}
