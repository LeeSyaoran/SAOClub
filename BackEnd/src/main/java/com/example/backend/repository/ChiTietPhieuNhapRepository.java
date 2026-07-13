package com.example.backend.repository;

import com.example.backend.entity.ChiTietPhieuNhap;
import com.example.backend.response.ChiTietPhieuNhapResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietPhieuNhapRepository extends JpaRepository<ChiTietPhieuNhap, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietPhieuNhapResponse(c.id, c.phieuNhapKho.phieuNhapId, c.bienThe.bienTheId, c.bienThe.maSku, c.soLuong, c.donGiaNhap, c.thanhTien) FROM ChiTietPhieuNhap c")
    List<ChiTietPhieuNhapResponse> hienThiChiTietPhieuNhap();

    // Xóa hết dòng chi tiết của 1 phiếu nhập — dùng khi xóa cả phiếu (PhieuNhapKhoService.delete).
    void deleteByPhieuNhapKho_PhieuNhapId(Integer phieuNhapId);

    // Dọn lịch sử phiếu nhập của 1 biến thể khi xóa hẳn biến thể/sản phẩm (chưa từng bán) —
    // đây chỉ là chứng từ chi phí mua hàng, không phải bằng chứng đã bán nên xóa an toàn.
    void deleteByBienThe_BienTheId(Integer bienTheId);
}
