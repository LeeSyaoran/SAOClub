package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "dm_o_cung")
public class DmOcung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "o_cung_id")
    @JsonProperty("oCungId")
    private Integer oCungId;

    @Column(name = "loai_o_cung", length = 100, nullable = false, unique = true)
    private String loaiOcung;

    @Column(name = "hinh_anh")
    private String hinhAnh;

    @JsonProperty("ocungId")
    public Integer getOcungIdAlias() {
        return oCungId;
    }

    @JsonProperty("ocungId")
    public void setOcungIdAlias(Integer id) {
        if (this.oCungId == null) this.oCungId = id;
    }
}
