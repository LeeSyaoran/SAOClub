package com.example.backend.service;

import com.example.backend.entity.KhachHang;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.NhanVienRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.response.HoSoResponse;
import com.example.backend.response.LoginResponse;
import com.example.backend.request.HoSoRequest;
import com.example.backend.security.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public LoginResponse buildLoginResponse(String username) {
        TaiKhoan tk = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

        String role = tk.getChucVu().getMaChucVu(); 
        String token = jwtUtil.generateToken(tk.getUsername(), role);

        if (tk.getNhanVien() != null) {
            NhanVien nv = tk.getNhanVien();
            return new LoginResponse(nv.getNhanVienId(), nv.getHoTen(), tk.getUsername(),
                    nv.getSoDienThoai(), nv.getEmail(), role, token);
        }

        if (tk.getKhachHang() != null) {
            KhachHang kh = tk.getKhachHang();
            return new LoginResponse(kh.getKhachHangId(), kh.getHoTen(), tk.getUsername(),
                    kh.getSoDienThoai(), kh.getEmail(), role, token);
        }

        throw new UsernameNotFoundException("Tài khoản không liên kết với người dùng: " + username);
    }

    public void doiMatKhau(String username, String matKhauCu, String matKhauMoi) {
        TaiKhoan tk = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));
        if (!passwordEncoder.matches(matKhauCu, tk.getMatKhauHash())) {
            throw new BadCredentialsException("Mật khẩu hiện tại không đúng");
        }
        tk.setMatKhauHash(passwordEncoder.encode(matKhauMoi));
        taiKhoanRepository.save(tk);
    }

    public HoSoResponse capNhatHoSo(String username, HoSoRequest req) {
        TaiKhoan tk = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));
        NhanVien nv = tk.getNhanVien();
        if (nv == null) {
            throw new IllegalStateException("Tài khoản này không có hồ sơ nhân viên để chỉnh sửa");
        }
        nv.setHoTen(req.getHoTen());
        nv.setSoDienThoai(req.getSoDienThoai());
        nv.setEmail(req.getEmail());
        nhanVienRepository.save(nv);
        return new HoSoResponse(nv.getHoTen(), nv.getSoDienThoai(), nv.getEmail());
    }
}
