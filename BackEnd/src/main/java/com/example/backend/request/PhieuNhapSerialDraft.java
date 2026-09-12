package com.example.backend.request;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PhieuNhapSerialDraft {
    private Integer bienTheId;
    private BigDecimal donGia;
    private List<String> serials;
}
