package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChiTietDonHangSerialResponse {
    private Integer chiTietDonHangId;
    private Integer chiTietId;
    private String soSerial;
}
