package com.example.backend.repository;

import com.example.backend.entity.KhuyenMai;
import com.example.backend.response.KhuyenMaiResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, Integer> {
    @Query("SELECT new com.example.backend.response.KhuyenMaiResponse(k.khuyenMaiId, k.maKhuyenMai, k.tenKhuyenMai, k.loai, k.giaTri, k.giaTriToiDa, k.donHangToiThieu, k.ngayBatDau, k.ngayKetThuc, k.soLuongToiDa, k.soLanDaDung, k.trangThai, k.ngayTao) FROM KhuyenMai k")
    List<KhuyenMaiResponse> hienThiKhuyenMai();

    // Khuyến mãi đang thực sự dùng được — active + trong khoảng ngày hiệu lực. Dùng chung
    // cho cả việc vẽ vòng quay (GET cau-hinh) lẫn random chọn thưởng (POST quay), tránh 2 nơi
    // lặp lại điều kiện lọc. Không loại theo donHangToiThieu — voucher trúng thưởng giữ nguyên
    // đơn tối thiểu của khuyến mãi gốc (xem VongQuayService.quay()), khách tự chịu trách nhiệm
    // đơn hàng có đạt điều kiện dùng được hay không, y hệt mã khuyến mãi công khai ở checkout.
    @Query("SELECT k FROM KhuyenMai k WHERE k.trangThai = 'active' " +
           "AND k.ngayBatDau <= CURRENT_TIMESTAMP AND k.ngayKetThuc >= CURRENT_TIMESTAMP")
    List<KhuyenMai> findActiveKhaDung();
}
