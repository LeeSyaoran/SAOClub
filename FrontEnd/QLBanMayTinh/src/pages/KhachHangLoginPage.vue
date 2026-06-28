<template>
  <div class="lp-root">

    <!-- ══ CỘT TRÁI: Brand ══ -->
    <div class="lp-brand">
      <!-- Glow blobs trang trí -->
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>

      <!-- Logo -->
      <div class="lp-logo">SAO<span>CLUB</span></div>

      <!-- Nội dung giữa -->
      <div class="lp-brand-body">
        <h1 class="lp-headline">Nơi hội tụ công nghệ<br>&amp; đặc quyền tối thượng.</h1>
        <p class="lp-tagline">Hàng nghìn laptop chính hãng, giao toàn quốc, bảo hành tận nơi.</p>

        <ul class="lp-feats">
          <li><span class="lp-diamond">◆</span> Chiết khấu VIP đến 5% mỗi đơn hàng</li>
          <li><span class="lp-diamond">◆</span> Miễn phí giao hàng hỏa tốc nội thành</li>
          <li><span class="lp-diamond">◆</span> Bảo hành chính hãng 1 đổi 1</li>
          <li><span class="lp-diamond">◆</span> Hỗ trợ kỹ thuật 24/7</li>
        </ul>
      </div>

      <!-- Laptop SVG minh họa -->
      <div class="lp-illus" aria-hidden="true">
        <svg viewBox="0 0 400 260" fill="none" xmlns="http://www.w3.org/2000/svg">
          <!-- Màn hình -->
          <rect x="60" y="20" width="280" height="185" rx="10" fill="#0f0f0f" stroke="rgba(255,255,255,0.08)" stroke-width="1.5"/>
          <rect x="70" y="30" width="260" height="165" rx="6" fill="#151515"/>
          <!-- Gradient glow màn hình -->
          <defs>
            <radialGradient id="glow" cx="50%" cy="50%" r="50%">
              <stop offset="0%" stop-color="#7c3aed" stop-opacity="0.35"/>
              <stop offset="60%" stop-color="#4f46e5" stop-opacity="0.15"/>
              <stop offset="100%" stop-color="#000" stop-opacity="0"/>
            </radialGradient>
          </defs>
          <rect x="70" y="30" width="260" height="165" rx="6" fill="url(#glow)"/>
          <!-- Đốm sáng màn hình -->
          <ellipse cx="200" cy="112" rx="90" ry="55" fill="rgba(124,58,237,0.18)"/>
          <ellipse cx="160" cy="90" rx="50" ry="30" fill="rgba(250,204,21,0.06)"/>
          <!-- Bàn phím -->
          <rect x="30" y="205" width="340" height="20" rx="4" fill="#1a1a1a" stroke="rgba(255,255,255,0.06)" stroke-width="1"/>
          <rect x="140" y="208" width="120" height="5" rx="2.5" fill="rgba(255,255,255,0.06)"/>
          <!-- Phím nhỏ -->
          <g fill="rgba(255,255,255,0.07)">
            <rect x="80"  y="212" width="12" height="4" rx="1"/>
            <rect x="96"  y="212" width="12" height="4" rx="1"/>
            <rect x="112" y="212" width="12" height="4" rx="1"/>
            <rect x="296" y="212" width="12" height="4" rx="1"/>
            <rect x="312" y="212" width="12" height="4" rx="1"/>
          </g>
          <!-- Chân laptop -->
          <path d="M30 225 L0 245 H400 L370 225Z" fill="#111" stroke="rgba(255,255,255,0.04)" stroke-width="1"/>
        </svg>
      </div>

      <div class="lp-brand-footer">© 2025 SAOClub. Tất cả quyền được bảo lưu.</div>
    </div>

    <!-- ══ CỘT PHẢI: Form ══ -->
    <div class="lp-form-wrap">

      <!-- Nút quay về (top left) -->
      <button class="lp-back" @click="goHome">← Quay về</button>

      <div class="lp-card">

        <!-- Tab -->
        <div class="lp-tabs">
          <button :class="['lp-tab', tab === 'login' && 'active']" @click="tab = 'login'">Đăng nhập</button>
          <button :class="['lp-tab', tab === 'register' && 'active']" @click="tab = 'register'">Đăng ký</button>
        </div>

        <Transition name="tf" mode="out-in">

          <!-- ── ĐĂNG NHẬP ── -->
          <form v-if="tab === 'login'" key="login" @submit.prevent="handleLogin">

            <h2 class="lp-title">Đăng nhập</h2>
            <p class="lp-sub">Chào mừng bạn quay trở lại với SaoClub</p>

            <div class="lp-field">
              <input v-model="lf.username" type="text"
                     class="lp-input" placeholder="Tài khoản" required autocomplete="username" />
            </div>

            <div class="lp-field">
              <div class="lp-pwd-wrap">
                <input v-model="lf.password"
                       :type="showPwd ? 'text' : 'password'"
                       class="lp-input" placeholder="Mật khẩu" required autocomplete="current-password" />
                <button type="button" class="lp-eye" @click="showPwd = !showPwd" tabindex="-1">
                  {{ showPwd ? '🙈' : '👁' }}
                </button>
              </div>
              <div class="lp-forgot-row">
                <a href="#" class="lp-forgot">Quên mật khẩu?</a>
              </div>
            </div>

            <div v-if="loginErr" class="lp-alert-err">{{ loginErr }}</div>

            <button type="submit" class="lp-cta" :disabled="loginBusy">
              {{ loginBusy ? 'ĐANG XỬ LÝ...' : 'ĐĂNG NHẬP NGAY' }}
            </button>

            <div class="lp-divider"><span>Hoặc đăng nhập bằng</span></div>

            <div class="lp-socials">
              <button type="button" class="lp-social-btn">
                <svg width="18" height="18" viewBox="0 0 48 48" fill="none" style="margin-right:8px">
                  <path d="M43.6 20.1H42V20H24v8h11.3C33.7 32.7 29.2 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.8 1.1 8 2.9l5.7-5.7C34.5 6.9 29.5 4.8 24 4.8 12.4 4.8 3 14.2 3 25.8S12.4 46.8 24 46.8 45 37.4 45 25.8c0-1.9-.2-3.7-.4-5.7z" fill="#FFC107"/>
                  <path d="M6.3 15.2l6.6 4.8C14.6 16.5 19 13.8 24 13.8c3.1 0 5.8 1.1 8 2.9l5.7-5.7C34.5 8.5 29.5 6.4 24 6.4c-7.7 0-14.3 4.4-17.7 10.8z" fill="#FF3D00"/>
                  <path d="M24 45.2c5.3 0 10.1-2 13.8-5.2l-6.4-5.4C29.4 36.3 26.8 37.2 24 37.2c-5.1 0-9.5-3.3-11.1-7.8l-6.5 5C9.6 41.3 16.3 45.2 24 45.2z" fill="#4CAF50"/>
                  <path d="M43.6 22.1H42V22H24v8h11.3c-.8 2.1-2.1 3.9-3.9 5.2l6.4 5.4C37.7 40.7 45 33.8 45 25.8c0-1.3-.1-2.5-.4-3.7z" fill="#1976D2"/>
                </svg>
                Google
              </button>
              <button type="button" class="lp-social-btn">
                <svg width="18" height="18" viewBox="0 0 48 48" fill="none" style="margin-right:8px">
                  <rect width="48" height="48" rx="10" fill="#0068FF"/>
                  <path d="M24 8C15.2 8 8 14.8 8 23.2c0 5.3 2.8 10 7 13v5.8L21.2 38c.9.2 1.8.3 2.8.3 8.8 0 16-6.8 16-15.2C40 14.8 32.8 8 24 8z" fill="white"/>
                </svg>
                Zalo
              </button>
            </div>

            <p class="lp-switch">
              Chưa có tài khoản?
              <button type="button" class="lp-link" @click="tab = 'register'">Đăng ký ngay</button>
            </p>

          </form>

          <!-- ── ĐĂNG KÝ ── -->
          <form v-else key="register" @submit.prevent="handleRegister">

            <h2 class="lp-title">Tạo tài khoản</h2>
            <p class="lp-sub">Đăng ký để nhận ưu đãi và tích điểm thành viên</p>

            <div class="lp-two-col">
              <div class="lp-field">
                <input v-model="rf.hoTen" type="text" class="lp-input" placeholder="Họ và tên *" required />
              </div>
              <div class="lp-field">
                <input v-model="rf.soDienThoai" type="tel" class="lp-input" placeholder="Số điện thoại *" required />
              </div>
            </div>

            <div class="lp-field">
              <input v-model="rf.email" type="email" class="lp-input" placeholder="Email" />
            </div>

            <div class="lp-field">
              <input v-model="rf.diaChi" type="text" class="lp-input" placeholder="Địa chỉ" />
            </div>

            <div class="lp-two-col">
              <div class="lp-field">
                <input v-model="rf.username" type="text" class="lp-input" placeholder="Tên đăng nhập *" required />
              </div>
              <div class="lp-field">
                <div class="lp-pwd-wrap">
                  <input v-model="rf.password"
                         :type="showRegPwd ? 'text' : 'password'"
                         class="lp-input" placeholder="Mật khẩu *" required />
                  <button type="button" class="lp-eye" @click="showRegPwd = !showRegPwd" tabindex="-1">
                    {{ showRegPwd ? '🙈' : '👁' }}
                  </button>
                </div>
              </div>
            </div>

            <div v-if="regErr"     class="lp-alert-err">{{ regErr }}</div>
            <div v-if="regSuccess" class="lp-alert-ok">{{ regSuccess }}</div>

            <button type="submit" class="lp-cta" :disabled="regBusy">
              {{ regBusy ? 'ĐANG ĐĂNG KÝ...' : 'ĐĂNG KÝ NGAY' }}
            </button>

            <p class="lp-switch">
              Đã có tài khoản?
              <button type="button" class="lp-link" @click="tab = 'login'">Đăng nhập</button>
            </p>

          </form>

        </Transition>
      </div><!-- /lp-card -->
    </div><!-- /lp-form-wrap -->

  </div><!-- /lp-root -->
</template>

<script setup>
import { ref, reactive } from 'vue';
import * as KhachHangService from '../Service/KhachHangService.js';

const emit = defineEmits(['go-home', 'login-success']);

const tab = ref('login');

// ── Đăng nhập ──────────────────────────────────────────────────────────────
const lf         = reactive({ username: '', password: '' });
const showPwd    = ref(false);
const loginBusy  = ref(false);
const loginErr   = ref('');

const handleLogin = async () => {
  loginErr.value = '';
  loginBusy.value = true;
  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: lf.username, password: lf.password }),
    });
    if (!res.ok) {
      loginErr.value = await res.text();
      return;
    }
    const user = await res.json();
    // Lưu session vào localStorage
    localStorage.setItem('saophone_session', JSON.stringify({
      id: user.id, name: user.hoTen, username: user.username,
      phone: user.soDienThoai, email: user.email, role: user.role,
    }));
    emit('login-success', user);
  } catch {
    loginErr.value = 'Không thể kết nối đến máy chủ.';
  } finally {
    loginBusy.value = false;
  }
};

// ── Đăng ký ────────────────────────────────────────────────────────────────
const rf         = reactive({ hoTen: '', soDienThoai: '', email: '', diaChi: '', username: '', password: '' });
const showRegPwd = ref(false);
const regBusy    = ref(false);
const regErr     = ref('');
const regSuccess = ref('');

const handleRegister = async () => {
  regErr.value     = '';
  regSuccess.value = '';
  const phone = rf.soDienThoai.replace(/\s/g, '');
  if (!/^[0-9]{10}$/.test(phone)) { regErr.value = 'Số điện thoại phải gồm 10 chữ số.'; return; }
  if (rf.password.length < 4)      { regErr.value = 'Mật khẩu tối thiểu 4 ký tự.'; return; }

  regBusy.value = true;
  try {
    const res = await KhachHangService.register({ ...rf, soDienThoai: phone });
    if (!res.ok) { regErr.value = await res.text(); return; }
    regSuccess.value = 'Đăng ký thành công! Hãy đăng nhập.';
    Object.assign(rf, { hoTen: '', soDienThoai: '', email: '', diaChi: '', username: '', password: '' });
    setTimeout(() => { tab.value = 'login'; regSuccess.value = ''; }, 1800);
  } catch {
    regErr.value = 'Không thể kết nối đến máy chủ.';
  } finally {
    regBusy.value = false;
  }
};

const goHome = () => emit('go-home');
</script>

<style scoped>
/* ══ Root ══ */
.lp-root {
  display: flex;
  min-height: 100vh;
  font-family: 'Inter', 'Segoe UI', sans-serif;
  background: #0e0e0e;
}

/* ══ Brand panel (LEFT) ══ */
.lp-brand {
  position: relative;
  width: 50%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 52px;
  background: linear-gradient(150deg, #1e0a3c 0%, #0d1240 45%, #0a0a1a 100%);
  overflow: hidden;
  color: #e5e7eb;
}

/* Trang trí glow blobs */
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
}
.blob-1 {
  width: 380px; height: 380px;
  background: rgba(124, 58, 237, 0.25);
  top: -80px; left: -80px;
}
.blob-2 {
  width: 300px; height: 300px;
  background: rgba(250, 204, 21, 0.08);
  bottom: 60px; right: -60px;
}

.lp-logo {
  position: relative;
  font-size: 2.2rem;
  font-weight: 900;
  letter-spacing: 1px;
  color: #fff;
  z-index: 1;
}
.lp-logo span { color: #facc15; }

.lp-brand-body { position: relative; z-index: 1; }

.lp-headline {
  font-size: 2.2rem;
  font-weight: 800;
  line-height: 1.25;
  color: #fff;
  margin: 0 0 14px;
}

.lp-tagline {
  font-size: 1rem;
  color: #9ca3af;
  margin: 0 0 32px;
  line-height: 1.6;
}

.lp-feats {
  list-style: none;
  padding: 0; margin: 0;
  display: flex; flex-direction: column; gap: 16px;
}
.lp-feats li {
  display: flex; align-items: center; gap: 12px;
  font-size: 0.95rem; color: #d1d5db;
}
.lp-diamond { color: #facc15; font-size: 0.65rem; flex-shrink: 0; }

/* SVG laptop illustration */
.lp-illus {
  position: relative;
  z-index: 1;
  opacity: 0.7;
  margin: 0 -20px;
}
.lp-illus svg { width: 100%; height: auto; }

.lp-brand-footer {
  position: relative;
  z-index: 1;
  font-size: 0.78rem;
  color: #374151;
}

/* ══ Form panel (RIGHT) ══ */
.lp-form-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;
  background: #111;
  position: relative;
}

/* Quay về */
.lp-back {
  position: absolute;
  top: 28px; left: 28px;
  background: none;
  border: none;
  color: #6b7280;
  font-size: 0.875rem;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
  transition: color 0.2s, background 0.2s;
}
.lp-back:hover { color: #facc15; background: rgba(250,204,21,0.08); }

/* Card */
.lp-card {
  width: 100%;
  max-width: 440px;
}

/* Tabs */
.lp-tabs {
  display: flex;
  background: #1a1a1a;
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 12px;
  padding: 5px;
  gap: 4px;
  margin-bottom: 28px;
}
.lp-tab {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #6b7280;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.lp-tab.active { background: #facc15; color: #000; }

/* Heading */
.lp-title {
  font-size: 1.7rem;
  font-weight: 800;
  color: #fff;
  margin: 0 0 6px;
}
.lp-sub {
  font-size: 0.875rem;
  color: #6b7280;
  margin: 0 0 24px;
}

/* Fields */
.lp-field { margin-bottom: 14px; }

.lp-input {
  width: 100%;
  height: 50px;
  padding: 0 16px;
  background: #1a1a1a;
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 10px;
  color: #f3f4f6;
  font-size: 0.95rem;
  box-sizing: border-box;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.lp-input:focus {
  outline: none;
  border-color: #facc15;
  box-shadow: 0 0 0 3px rgba(250,204,21,0.12);
}
.lp-input::placeholder { color: #4b5563; }

.lp-pwd-wrap { position: relative; }
.lp-pwd-wrap .lp-input { padding-right: 46px; }

.lp-eye {
  position: absolute;
  right: 14px; top: 50%;
  transform: translateY(-50%);
  background: none; border: none;
  color: #4b5563; cursor: pointer;
  font-size: 1rem; line-height: 1;
  transition: color 0.2s;
}
.lp-eye:hover { color: #facc15; }

.lp-forgot-row { text-align: right; margin-top: 6px; }
.lp-forgot {
  font-size: 0.8rem;
  color: #facc15;
  text-decoration: none;
  font-weight: 600;
}
.lp-forgot:hover { text-decoration: underline; }

/* Two columns */
.lp-two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

/* Alerts */
.lp-alert-err {
  background: rgba(239,68,68,0.1);
  border: 1px solid rgba(239,68,68,0.3);
  color: #fca5a5;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 0.85rem;
  margin-bottom: 12px;
}
.lp-alert-ok {
  background: rgba(34,197,94,0.1);
  border: 1px solid rgba(34,197,94,0.3);
  color: #86efac;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 0.85rem;
  margin-bottom: 12px;
}

/* CTA button */
.lp-cta {
  width: 100%;
  height: 50px;
  background: #facc15;
  color: #000;
  border: none;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 800;
  cursor: pointer;
  transition: background 0.2s, transform 0.15s, box-shadow 0.2s;
  margin-bottom: 18px;
  margin-top: 4px;
}
.lp-cta:hover:not(:disabled) {
  background: #fde047;
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(250,204,21,0.3);
}
.lp-cta:disabled { opacity: 0.6; cursor: not-allowed; }

/* Divider */
.lp-divider {
  display: flex; align-items: center; gap: 12px;
  color: #374151; font-size: 0.8rem; margin-bottom: 14px;
}
.lp-divider::before, .lp-divider::after {
  content: ''; flex: 1; height: 1px;
  background: rgba(255,255,255,0.07);
}

/* Social buttons */
.lp-socials { display: flex; gap: 10px; margin-bottom: 20px; }
.lp-social-btn {
  flex: 1; height: 44px;
  display: flex; align-items: center; justify-content: center;
  background: #1a1a1a;
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 10px;
  color: #d1d5db;
  font-size: 0.875rem; font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.lp-social-btn:hover { border-color: rgba(255,255,255,0.25); background: #222; }

/* Switch hint */
.lp-switch {
  text-align: center;
  font-size: 0.875rem;
  color: #6b7280;
  margin: 0;
}
.lp-link {
  background: none; border: none;
  color: #facc15; font-weight: 700;
  cursor: pointer; padding: 0;
  font-size: inherit;
}
.lp-link:hover { text-decoration: underline; }

/* ── Transitions ── */
.tf-enter-active, .tf-leave-active { transition: opacity 0.18s, transform 0.18s; }
.tf-enter-from { opacity: 0; transform: translateX(10px); }
.tf-leave-to   { opacity: 0; transform: translateX(-10px); }

/* ── Responsive ── */
@media (max-width: 900px) {
  .lp-brand { display: none; }
  .lp-form-wrap { padding: 80px 20px 40px; }
}
@media (max-width: 480px) {
  .lp-two-col { grid-template-columns: 1fr; }
  .lp-card { max-width: 100%; }
}
</style>
