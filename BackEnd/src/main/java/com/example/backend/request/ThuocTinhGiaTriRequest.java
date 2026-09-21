package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ThuocTinhGiaTriRequest {

    private Integer giaTriId;

    private Integer thuocTinhId;

    @NotBlank(message = "Giá trị không được trống")
    private String giaTri;

    private Integer thuTu = 0;
}
