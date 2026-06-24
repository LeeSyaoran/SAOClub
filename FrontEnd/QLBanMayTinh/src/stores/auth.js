import { reactive, computed } from "vue";

// ── Auth Store ────────────────────────────────────────────────────────────────
// Lưu trạng thái đăng nhập vào localStorage để không mất khi F5 trang

const STORAGE_KEY = "saophone_user";

// Đọc user đã lưu từ localStorage khi app khởi động
const savedUser = (() => {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY)); }
  catch { return null; }
})();

// State toàn cục — dùng reactive để Vue có thể theo dõi thay đổi
export const authStore = reactive({
  // Thông tin user hiện tại (null = chưa đăng nhập)
  user: savedUser ?? null,

  // Hiện/ẩn modal đăng nhập từ bất kỳ đâu
  showLoginModal: false,
});

// ── Computed helpers ──────────────────────────────────────────────────────────

// Kiểm tra đã đăng nhập chưa
export const isLoggedIn = computed(() => authStore.user !== null);

// Kiểm tra có phải admin không
export const isAdmin = computed(() => authStore.user?.role === "admin");

// ── Actions ───────────────────────────────────────────────────────────────────

// Đăng nhập — lưu user vào store + localStorage
export const login = (userData) => {
  authStore.user = {
    id:       userData.id       ?? 1,
    name:     userData.name     ?? userData.hoTen ?? "Người dùng",
    phone:    userData.phone    ?? userData.soDienThoai ?? "",
    email:    userData.email    ?? "",
    role:     userData.role     ?? "user",  // 'admin' | 'user'
    username: userData.username ?? "",
  };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(authStore.user));
  authStore.showLoginModal = false;
};

// Đăng xuất — xóa store + localStorage
export const logout = () => {
  authStore.user = null;
  localStorage.removeItem(STORAGE_KEY);
};

// Mở modal đăng nhập (gọi từ bất kỳ component nào)
export const requireLogin = () => {
  authStore.showLoginModal = true;
};
