package com.example.backend.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DmOcungResponse {
    @JsonProperty("oCungId")
    private Integer oCungId;
    private String loaiOcung;

    @JsonProperty("ocungId")
    public Integer getOcungIdAlias() {
        return oCungId;
    }

    @JsonProperty("ocungId")
    public void setOcungIdAlias(Integer id) {
        if (this.oCungId == null) this.oCungId = id;
    }
}
