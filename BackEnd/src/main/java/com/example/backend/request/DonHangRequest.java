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

    private Integer nhanVienId;

    private Integer khuyenMaiId;

    private Integer diaChiGiaoHangId;

    private String diaChiGiaoHangText;

    @NotBlank(message = "Người nhận không được để trống")
    private String nguoiNhan;

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    @Size(max = 20, message = "Số điện thoại không vượt quá 20 ký tự")
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

    // thanhTien là computed column trong DB (tong_tien - giam_gia + phi_van_chuyen) — không validate, không INSERT
    @PositiveOrZero(message = "Thành tiền phải lớn hơn hoặc bằng 0")
    private BigDecimal thanhTien;

    @NotNull(message = "Ngày đặt không được để trống")
    private LocalDateTime ngayDat;

    private LocalDateTime ngayGiaoDuKien;

    private LocalDateTime ngayGiaoThucTe;

    @NotBlank(message = "Trạng thái đơn hàng không được để trống")
    private String trangThaiDonHang;

    @NotBlank(message = "Trạng thái thanh toán không được để trống")
    private String trangThaiThanhToan;

    private String kenhBan;

    private String ghiChu;
}
