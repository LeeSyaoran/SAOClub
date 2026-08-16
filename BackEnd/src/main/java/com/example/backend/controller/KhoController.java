package com.example.backend.controller;

import com.example.backend.request.NhapKhoRequest;
import com.example.backend.service.KhoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Toàn bộ nghiệp vụ kho đều là dữ liệu nội bộ (giá vốn, serial, lịch sử nhập) nên chặn
 * quyền ở cấp class thay vì rải @PreAuthorize từng hàm.
 */
@RestController
@RequestMapping("/api/kho")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class KhoController {

    @Autowired
    private KhoService khoService;

    /** Bảng chính: mọi biến thể kèm tồn thực tế / đang giữ / có thể bán / tồn tối thiểu. */
    @GetMapping("/ton-kho")
    public List<Map<String, Object>> tonKho() {
        return khoService.danhSachTonKho();
    }

    @GetMapping("/serial")
    public List<Map<String, Object>> serial(@RequestParam Integer bienTheId) {
        return khoService.danhSachSerial(bienTheId);
    }

    @GetMapping("/lich-su")
    public List<Map<String, Object>> lichSu(@RequestParam Integer bienTheId) {
        return khoService.lichSuKho(bienTheId);
    }

    @GetMapping("/nhan-vien")
    public List<Map<String, Object>> nhanVien() {
        return khoService.danhSachNhanVien();
    }

    @GetMapping("/phieu-nhap")
    public List<Map<String, Object>> phieuNhap() {
        return khoService.danhSachPhieuNhap();
    }

    /** Nhập hàng: một phiếu, nhiều dòng, mỗi dòng kèm danh sách serial. */
    @PostMapping("/nhap-hang")
    public ResponseEntity<Map<String, Object>> nhapHang(@Valid @RequestBody NhapKhoRequest request) {
        return ResponseEntity.ok(khoService.nhapHang(request));
    }

    @PutMapping("/bien-the/{bienTheId}")
    public ResponseEntity<Void> capNhatBienThe(@PathVariable Integer bienTheId,
                                               @RequestBody Map<String, Object> body) {
        khoService.capNhatBienThe(bienTheId, body);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/serial/{chiTietId}")
    public ResponseEntity<Void> doiTrangThaiSerial(@PathVariable Integer chiTietId,
                                                   @RequestBody Map<String, Object> body) {
        khoService.doiTrangThaiSerial(chiTietId,
                String.valueOf(body.get("trangThai")),
                body.get("ghiChu") == null ? null : String.valueOf(body.get("ghiChu")),
                body.get("nhanVienId") == null ? null : Integer.valueOf(String.valueOf(body.get("nhanVienId"))));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/ton-kho/{bienTheId}")
    public ResponseEntity<Void> capNhatTonToiThieu(@PathVariable Integer bienTheId,
                                                   @RequestBody Map<String, Object> body) {
        khoService.capNhatTonToiThieu(bienTheId,
                Integer.valueOf(String.valueOf(body.get("tonToiThieu"))));
        return ResponseEntity.ok().build();
    }
}