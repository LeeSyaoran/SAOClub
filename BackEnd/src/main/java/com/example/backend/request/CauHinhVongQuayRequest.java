package com.example.backend.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class CauHinhVongQuayRequest {

    @NotNull(message = "Điểm mỗi lượt không được để trống")
    @Min(value = 1, message = "Điểm mỗi lượt phải lớn hơn 0")
    private Integer diemMoiLuot;

    @NotNull(message = "Tỷ lệ trượt không được để trống")
    @Min(value = 0, message = "Tỷ lệ trượt phải từ 0 đến 100")
    @Max(value = 100, message = "Tỷ lệ trượt phải từ 0 đến 100")
    private Integer tyLeTruot;
}
