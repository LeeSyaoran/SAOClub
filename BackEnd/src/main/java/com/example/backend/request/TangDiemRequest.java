package com.example.backend.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TangDiemRequest {
    @NotNull(message = "Số điểm không được để trống")
    @Positive(message = "Số điểm phải lớn hơn 0")
    private Integer soDiem;

    private String lyDo;
}
