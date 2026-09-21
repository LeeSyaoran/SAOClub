package com.example.backend.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class KhuyenMaiSanPhamRequest {
    private Integer khuyenMaiId;
    private List<Integer> sanPhamIds;
}
