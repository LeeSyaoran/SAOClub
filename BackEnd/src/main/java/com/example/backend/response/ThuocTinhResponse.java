package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThuocTinhResponse {

    private Integer thuocTinhId;
    private String tenTruong;
    private String tenHienThi;
    private String loaiDuLieu;
    private Boolean batBuoc;
    private Integer thuTuHienThi;
    private String trangThai;
    private List<GiaTriResponse> giaTriList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GiaTriResponse {
        private Integer giaTriId;
        private String giaTri;
        private Integer thuTu;
    }
}
