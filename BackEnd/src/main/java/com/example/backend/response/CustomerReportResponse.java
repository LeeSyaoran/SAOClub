package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class CustomerReportResponse {
    private List<CustomerSpendingResponse> topKhach;
    private double tyLeMuaLai;
    private int tongSoKhach;
}
