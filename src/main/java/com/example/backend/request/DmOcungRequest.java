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
public class DmOcungRequest {
    @NotBlank(message = "Loại ổ cứng không được để trống")
    private String loaiOcung;
}
