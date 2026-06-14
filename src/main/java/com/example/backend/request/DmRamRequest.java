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
public class DmRamRequest {
    @NotBlank(message = "Dung lượng RAM không được để trống")
    private String dungLuong;
}
