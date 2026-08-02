package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Tổng hợp đánh giá của 1 sản phẩm — dùng cho badge "⭐ 4.5 (12 đánh giá)" trên ProductCard/
// ProductDetail, tránh phải tải hết danh sách đánh giá chi tiết chỉ để hiện điểm trung bình.
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DanhGiaSummaryResponse {
    private Integer sanPhamId;
    private Double diemTrungBinh;
    private Long tongSoDanhGia;
}
