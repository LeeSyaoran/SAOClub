package com.example.backend.service;

import com.example.backend.entity.ChucVu;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.ChucVuRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.NhanVienRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.response.HoSoResponse;
import com.example.backend.response.LoginResponse;
import com.example.backend.request.HoSoRequest;
import com.example.backend.security.jwt.JwtUtil;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private ChucVuRepository chucVuRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public LoginResponse getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UsernameNotFoundException("Chưa đăng nhập");
        }
        String username = auth.getName();
        return buildLoginResponse(username);
    }

    public LoginResponse buildLoginResponse(String username) {
        TaiKhoan tk = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

        String role = tk.getChucVu().getMaChucVu(); 
        String token = jwtUtil.generateToken(tk.getUsername(), role);

        if (tk.getNhanVien() != null) {
            NhanVien nv = tk.getNhanVien();
            return new LoginResponse(nv.getNhanVienId(), nv.getHoTen(), tk.getUsername(),
                    nv.getSoDienThoai(), nv.getEmail(), role, token, tk.getAvatarUrl());
        }

        if (tk.getKhachHang() != null) {
            KhachHang kh = tk.getKhachHang();
            return new LoginResponse(kh.getKhachHangId(), kh.getHoTen(), tk.getUsername(),
                    kh.getSoDienThoai(), kh.getEmail(), role, token, tk.getAvatarUrl());
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

    public LoginResponse capNhatHoSo(String username, HoSoRequest req) {
        TaiKhoan tk = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

        if (tk.getNhanVien() != null) {
            NhanVien nv = tk.getNhanVien();
            nv.setHoTen(req.getHoTen());
            nv.setSoDienThoai(req.getSoDienThoai());
            nv.setEmail(req.getEmail());
            nhanVienRepository.save(nv);
            if (req.getAvatarUrl() != null && !req.getAvatarUrl().isEmpty()) {
                tk.setAvatarUrl(req.getAvatarUrl());
                taiKhoanRepository.save(tk);
            }
        } else if (tk.getKhachHang() != null) {
            KhachHang kh = tk.getKhachHang();
            kh.setHoTen(req.getHoTen());
            kh.setSoDienThoai(req.getSoDienThoai());
            kh.setEmail(req.getEmail());
            khachHangRepository.save(kh);
            if (req.getAvatarUrl() != null && !req.getAvatarUrl().isEmpty()) {
                tk.setAvatarUrl(req.getAvatarUrl());
                taiKhoanRepository.save(tk);
            }
        } else {
            throw new IllegalStateException("Tài khoản không có hồ sơ để chỉnh sửa");
        }

        // Trả LoginResponse với JWT mới sau khi update
        return buildLoginResponse(username);
    }

    @Transactional
    public LoginResponse firebaseLogin(String idToken, String provider) {
        try {
            // Verify Firebase token
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            String picture = decodedToken.getPicture();

            // Find existing account by provider + uid
            var existingAccount = taiKhoanRepository.findByProviderAndProviderUid(provider, uid);

            if (existingAccount.isPresent()) {
                // Existing user - login
                TaiKhoan tk = existingAccount.get();
                return buildLoginResponse(tk.getUsername());
            }

            // New user - create account
            ChucVu khachHangRole = chucVuRepository.findByMaChucVu("khach_hang")
                    .orElseThrow(() -> new RuntimeException("Role khach_hang not found"));

            // Create KhachHang profile
            // so_dien_thoai NULL được (migration mới: filtered unique index cho phép nhiều NULL).
            // User Firebase sẽ tự cập nhật SĐT thật khi mua hàng — checkout sẽ bắt buộc điền.
            KhachHang kh = new KhachHang();
            kh.setHoTen(name != null ? name : (email != null ? email : "Khách hàng"));
            kh.setEmail(email);
            kh.setSoDienThoai(null);     // cho phép null; user cập nhật trong hồ sơ / khi checkout
            kh.setDiaChi(null);          // NULL trong DB
            kh.setHinhAnh(null);         // NULL trong DB
            kh.setLoaiKhach("ca_nhan");  // NOT NULL
            kh.setDiemTichLuy(0);        // NOT NULL
            kh.setTrangThai("active");   // NOT NULL DEFAULT 'active'
            kh.setDaXoa(false);          // NOT NULL DEFAULT 0
            kh.setSoDuVi(java.math.BigDecimal.ZERO); // NOT NULL DEFAULT 0
            kh.setNgayTao(LocalDateTime.now());
            kh = khachHangRepository.save(kh);

            // Create TaiKhoan with Firebase link
            TaiKhoan tk = new TaiKhoan();
            tk.setUsername(provider + "_" + uid.substring(0, Math.min(20, uid.length())));
            tk.setMatKhauHash(passwordEncoder.encode(UUID.randomUUID().toString())); // Random password
            tk.setChucVu(khachHangRole);
            tk.setKhachHang(kh);
            tk.setTrangThai("active");
            tk.setNgayTao(LocalDateTime.now());
            tk.setProvider(provider);
            tk.setProviderUid(uid);
            tk.setAvatarUrl(picture);
            tk = taiKhoanRepository.save(tk);

            return buildLoginResponse(tk.getUsername());

        } catch (FirebaseAuthException e) {
            throw new BadCredentialsException("Firebase token không hợp lệ: " + e.getMessage());
        }
    }
}
