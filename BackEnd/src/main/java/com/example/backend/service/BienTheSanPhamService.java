package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.repository.*;
import com.example.backend.request.BienTheSanPhamRequest;
import com.example.backend.response.BienTheSanPhamResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BienTheSanPhamService {

    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private DmCpuRepository dmCpuRepository;
    @Autowired
    private DmRamRepository dmRamRepository;
    @Autowired
    private DmOcungRepository dmOcungRepository;
    @Autowired
    private DmGpuRepository dmGpuRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;
    @Autowired
    private ChiTietTraHangRepository chiTietTraHangRepository;
    @Autowired
    private PhieuBaoHanhRepository phieuBaoHanhRepository;
    @Autowired
    private ChiTietPhieuNhapRepository chiTietPhieuNhapRepository;
    @Autowired
    private TonKhoRepository tonKhoRepository;
    @Autowired
    private LichSuTonKhoRepository lichSuTonKhoRepository;

    public List<BienTheSanPhamResponse> hienThiBienTheSanPham() {
        return bienTheSanPhamRepository.hienThiBienTheSanPham();
    }

    public BienTheSanPham getById(Integer id) {
        return bienTheSanPhamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Biến thể sản phẩm không tồn tại với id: " + id));
    }

    public BienTheSanPham create(BienTheSanPhamRequest request) {
        BienTheSanPham entity = new BienTheSanPham();
        // Bỏ qua: sanPhamId, cpuId, ramId, oCungId, gpuId (khác tên với entity)
        BeanUtils.copyProperties(request, entity, "sanPhamId", "cpuId", "ramId", "oCungId", "gpuId");

        entity.setSanPham(sanPhamRepository.getReferenceById(request.getSanPhamId()));
        entity.setCpu(request.getCpuId() != null ? dmCpuRepository.getReferenceById(request.getCpuId()) : null);
        entity.setRam(request.getRamId() != null ? dmRamRepository.getReferenceById(request.getRamId()) : null);
        entity.setOCung(request.getOCungId() != null ? dmOcungRepository.getReferenceById(request.getOCungId()) : null);
        entity.setGpu(request.getGpuId() != null ? dmGpuRepository.getReferenceById(request.getGpuId()) : null);

        return bienTheSanPhamRepository.save(entity);
    }

    public BienTheSanPham update(Integer id, BienTheSanPhamRequest request) {
        BienTheSanPham entity = getById(id);
        BeanUtils.copyProperties(request, entity, "bienTheId", "sanPhamId", "cpuId", "ramId", "oCungId", "gpuId");

        entity.setSanPham(sanPhamRepository.getReferenceById(request.getSanPhamId()));
        entity.setCpu(request.getCpuId() != null ? dmCpuRepository.getReferenceById(request.getCpuId()) : null);
        entity.setRam(request.getRamId() != null ? dmRamRepository.getReferenceById(request.getRamId()) : null);
        entity.setOCung(request.getOCungId() != null ? dmOcungRepository.getReferenceById(request.getOCungId()) : null);
        entity.setGpu(request.getGpuId() != null ? dmGpuRepository.getReferenceById(request.getGpuId()) : null);

        return bienTheSanPhamRepository.save(entity);
    }

    // Biến thể đã qua giao dịch chưa (đã bán/trả/bảo hành, hoặc có serial không còn
    // "trong_kho") — dùng để FE hỏi trước khi hiện hộp thoại xóa (câu hỏi đơn giản nếu an
    // toàn, cảnh báo rõ lý do nếu không) và để delete() tự chặn nếu ai đó gọi thẳng API.
    public boolean hasTransactionHistory(Integer id) {
        return chiTietDonHangRepository.existsByBienThe_BienTheId(id)
                || chiTietTraHangRepository.existsByBienThe_BienTheId(id)
                || phieuBaoHanhRepository.existsByBienThe_BienTheId(id)
                || chiTietSanPhamRepository.existsByBienThe_BienTheIdAndTrangThaiNot(id, "trong_kho");
    }

    // Chỉ xóa được biến thể CHƯA từng qua giao dịch — có giao dịch rồi mà xóa sẽ làm sai
    // lịch sử đơn hàng/bảo hành đã ghi nhận. Nếu an toàn, dọn hết dữ liệu phụ thuộc (tồn
    // kho/serial/lịch sử/phiếu nhập của riêng biến thể này — không phải bằng chứng đã bán)
    // trước khi xóa biến thể.
    @Transactional
    public void delete(Integer id) {
        BienTheSanPham entity = getById(id);
        if (hasTransactionHistory(id)) {
            throw new IllegalArgumentException(
                    "Không thể xóa biến thể \"" + entity.getMaSku() + "\": đã có lịch sử bán hàng/bảo hành. " +
                    "Hãy chuyển trạng thái sang ngừng kinh doanh thay vì xóa.");
        }
        chiTietSanPhamRepository.deleteByBienThe_BienTheId(id);
        tonKhoRepository.deleteByBienThe_BienTheId(id);
        chiTietPhieuNhapRepository.deleteByBienThe_BienTheId(id);
        lichSuTonKhoRepository.deleteByBienThe_BienTheId(id);
        bienTheSanPhamRepository.deleteById(id);
    }
}
