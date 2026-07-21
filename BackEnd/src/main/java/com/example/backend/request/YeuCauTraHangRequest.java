package com.example.backend.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class YeuCauTraHangRequest {
    @NotNull(message = "Đơn hàng không được để trống")
    private Integer donHangId;

    @NotBlank(message = "Lý do không được để trống")
    private String lyDo;

    @NotEmpty(message = "Phải chọn ít nhất 1 sản phẩm để trả")
    @Valid
    private List<DongTraRequest> dongTra;
}
