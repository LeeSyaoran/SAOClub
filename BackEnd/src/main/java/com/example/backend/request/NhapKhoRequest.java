package com.example.backend.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Một phiếu nhập kho = nhiều dòng, mỗi dòng là một biến thể kèm danh sách serial.
 * Quy ước: dòng nào theoSerial = true thì số serial phải khớp đúng soLuong.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class NhapKhoRequest {

    @NotNull(message = "Chưa chọn nhà cung cấp")
    private Integer nhaCungCapId;

    private Integer nhanVienId;

    private LocalDateTime ngayNhap;

    private String ghiChu;

    /** Cập nhật gia_nhap của biến thể theo đơn giá của phiếu này. */
    private boolean capNhatGiaNhap;

    @NotEmpty(message = "Phiếu nhập phải có ít nhất một dòng hàng")
    @Valid
    private List<DongNhap> dongNhap = new ArrayList<>();

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class DongNhap {

        @NotNull(message = "Chưa chọn biến thể sản phẩm")
        private Integer bienTheId;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private Integer soLuong;

        @NotNull(message = "Đơn giá nhập không được để trống")
        @PositiveOrZero(message = "Đơn giá nhập phải lớn hơn hoặc bằng 0")
        private BigDecimal donGiaNhap;

        /** true: nhập theo serial (laptop, điện thoại). false: chỉ cộng số lượng (phụ kiện). */
        private boolean theoSerial = true;

        private List<String> serials = new ArrayList<>();

        private String ghiChu;
    }
}