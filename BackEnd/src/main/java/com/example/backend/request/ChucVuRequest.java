package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChucVuRequest {
    @NotBlank(message = "Tên chức vụ không được để trống")
    private String tenChucVu;

    private String moTa;
}
