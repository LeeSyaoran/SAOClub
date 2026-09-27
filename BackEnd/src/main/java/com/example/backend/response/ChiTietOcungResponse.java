package com.example.backend.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChiTietOcungResponse {
    private Integer chiTietOCungId;

    @JsonProperty("oCungId")
    private Integer oCungId;

    private String loaiOcung;
    private String soSerial;
    private String trangThai;
    private LocalDateTime ngayNhapKho;
    private String ghiChu;

    @JsonProperty("ocungId")
    public Integer getOcungIdAlias() {
        return oCungId;
    }

    @JsonProperty("ocungId")
    public void setOcungIdAlias(Integer id) {
        if (this.oCungId == null) this.oCungId = id;
    }
}
