package com.example.backend.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DongGoiRequest {
    @NotEmpty(message = "Đơn hàng chưa có dòng sản phẩm nào")
    @Valid
    private List<DongGoiLineRequest> lines;
}
