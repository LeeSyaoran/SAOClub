package com.example.backend.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SanPhamRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String tenSanPham;

    @NotNull(message = "Thương hiệu không được để trống")
    private Integer thuongHieuId;

    @NotNull(message = "Danh mục không được để trống")
    private Integer danhMucId;

    @NotNull(message = "Nhà cung cấp không được để trống")
    private Integer nhaCungCapId;

    @NotBlank(message = "Mã SKU không được để trống")
    private String maSku;

    @NotBlank(message = "CPU không được để trống")
    private String cpu;

    @NotBlank(message = "RAM không được để trống")
    private String ram;

    @NotBlank(message = "Ổ cứng không được để trống")
    private String oCung;

    @NotBlank(message = "GPU không được để trống")
    private String gpu;

    @NotBlank(message = "Kích thước màn hình không được để trống")
    private String kichThuocManHinh;

    @NotBlank(message = "Hệ điều hành không được để trống")
    private String heDieuHanh;

    @NotBlank(message = "Pin không được để trống")
    private String pin;

    @NotNull(message = "Trọng lượng không được để trống")
    @PositiveOrZero(message = "Trọng lượng phải lớn hơn hoặc bằng 0")
    private BigDecimal trongLuongKg;

    @NotBlank(message = "Màu sắc không được để trống")
    private String mauSac;

    @NotNull(message = "Giá bán không được để trống")
    @PositiveOrZero(message = "Giá bán phải lớn hơn hoặc bằng 0")
    private BigDecimal giaBan;

    @NotNull(message = "Giá nhập không được để trống")
    @PositiveOrZero(message = "Giá nhập phải lớn hơn hoặc bằng 0")
    private BigDecimal giaNhap;

    @NotNull(message = "Bảo hành tháng không được để trống")
    @PositiveOrZero(message = "Bảo hành tháng phải lớn hơn hoặc bằng 0")
    private Integer baoHanhThang;

    @NotBlank(message = "Mô tả không được để trống")
    private String moTa;

    @NotBlank(message = "Hình ảnh chính không được để trống")
    private String hinhAnhChinh;

    @NotBlank(message = "Trạng thái không được để trống")
    private String trangThai;

    @NotNull(message = "Ngày tạo không được để trống")
    private LocalDateTime ngayTao;
}
