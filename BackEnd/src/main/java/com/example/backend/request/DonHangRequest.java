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
public class DonHangRequest {
    @NotNull(message = "Khách hàng không được để trống")
    private Integer khachHangId;

    @NotNull(message = "Nhân viên không được để trống")
    private Integer nhanVienId;

    @NotNull(message = "Khuyến mãi không được để trống")
    private Integer khuyenMaiId;

    @NotNull(message = "Địa chỉ giao hàng không được để trống")
    private Integer diaChiGiaoHangId;

    @NotBlank(message = "Địa chỉ giao hàng text không được để trống")
    private String diaChiGiaoHangText;

    @NotBlank(message = "Người nhận không được để trống")
    private String nguoiNhan;

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    private String sdtNguoiNhan;

    @NotNull(message = "Tổng tiền không được để trống")
    @PositiveOrZero(message = "Tổng tiền phải lớn hơn hoặc bằng 0")
    private BigDecimal tongTien;

    @NotNull(message = "Giảm giá không được để trống")
    @PositiveOrZero(message = "Giảm giá phải lớn hơn hoặc bằng 0")
    private BigDecimal giamGia;

    @NotNull(message = "Phí vận chuyển không được để trống")
    @PositiveOrZero(message = "Phí vận chuyển phải lớn hơn hoặc bằng 0")
    private BigDecimal phiVanChuyen;

    @NotNull(message = "Thành tiền không được để trống")
    @PositiveOrZero(message = "Thành tiền phải lớn hơn hoặc bằng 0")
    private BigDecimal thanhTien;

    @NotNull(message = "Ngày đặt không được để trống")
    private LocalDateTime ngayDat;

    @NotNull(message = "Ngày giao dự kiến không được để trống")
    private LocalDateTime ngayGiaoDuKien;

    @NotNull(message = "Ngày giao thực tế không được để trống")
    private LocalDateTime ngayGiaoThucTe;

    @NotBlank(message = "Trạng thái đơn hàng không được để trống")
    private String trangThaiDonHang;

    @NotBlank(message = "Trạng thái thanh toán không được để trống")
    private String trangThaiThanhToan;

    @NotBlank(message = "Kênh bán không được để trống")
    private String kenhBan;

    @NotBlank(message = "Ghi chú không được để trống")
    private String ghiChu;
}
