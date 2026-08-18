package com.example.backend.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SanPhamRequest {

    private Integer bienTheId;

    // Mã nội bộ hiển thị/tra cứu trên UI (SP0001...). Để trống thì service tự sinh theo id vừa lưu
    @Size(max = 50, message = "Mã sản phẩm tối đa 50 ký tự")
    private String maSanPham;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String tenSanPham;

    @NotNull(message = "Thương hiệu không được để trống")
    private Integer thuongHieuId;

    @NotNull(message = "Danh mục không được để trống")
    private Integer danhMucId;

    private Integer nhaCungCapId;

    @NotBlank(message = "Loại sản phẩm không được để trống")
    private String loaiSanPham;

    @NotBlank(message = "Mã SKU không được để trống")
    private String maSku;

    // Barcode chuẩn thuộc về BIẾN THỂ (vì sản phẩm cha không dùng barcode nữa)
    @Pattern(regexp = "^$|^\\d{8,13}$", message = "Barcode phải gồm 8–13 chữ số")
    private String barcodeBienThe;

    private Integer cpuId;
    private Integer ramId;
    private Integer oCungId;
    private Integer gpuId;

    private String kichThuocManHinh;
    private String heDieuHanh;
    private String pin;

    @PositiveOrZero(message = "Trọng lượng phải lớn hơn hoặc bằng 0")
    private BigDecimal trongLuongKg;

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

    private String moTa;
    private String hinhAnhChinh;

    // Danh sách toàn bộ ảnh gallery
    private List<String> hinhAnhList;

    @NotBlank(message = "Trạng thái không được để trống")
    private String trangThai;

    private LocalDateTime ngayTao;

    private String hinhAnhBienThe;
    private String phanLoaiTags;
    private String phanLoaiTen;
}