package com.example.backend.request;

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
public class DongGoiLineRequest {
    @NotNull(message = "Dòng đơn hàng không được để trống")
    private Integer chiTietDonHangId;

    @NotEmpty(message = "Phải chọn ít nhất 1 serial")
    private List<Integer> serialIds;
}
