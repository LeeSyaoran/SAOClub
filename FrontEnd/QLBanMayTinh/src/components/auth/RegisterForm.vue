<template>
  <div class="register-overlay">
    <div class="register-card">

      <!-- Nút đóng -->
      <button
          class="close-btn"
          @click="emit('close')"
      >
        ✕
      </button>

      <!-- Logo -->
      <div class="logo">
        SAO<span>PHONE</span>
      </div>

      <h2>Đăng ký tài khoản</h2>

      <p class="sub-title">
        Tạo tài khoản để mua sắm, theo dõi đơn hàng và nhận nhiều ưu đãi hấp dẫn.
      </p>

      <!-- Form -->
      <form
          class="register-form"
          @submit.prevent="registerUser"
      >

        <!-- Họ tên -->
        <div class="form-item">
          <label>Họ và tên</label>

          <div class="input-box">
            <span class="icon">👤</span>

            <input
                v-model="form.hoTen"
                type="text"
                placeholder="Nhập họ và tên"
            />
          </div>
        </div>

        <!-- SĐT -->
        <div class="form-item">
          <label>Số điện thoại</label>

          <div class="input-box">
            <span class="icon">📱</span>

            <input
                v-model="form.soDienThoai"
                type="text"
                placeholder="Nhập số điện thoại"
            />
          </div>
        </div>

        <!-- Email -->
        <div class="form-item">
          <label>Email</label>

          <div class="input-box">
            <span class="icon">✉️</span>

            <input
                v-model="form.email"
                type="email"
                placeholder="example@gmail.com"
            />
          </div>
        </div>

        <!-- Username -->
        <div class="form-item">
          <label>Tên đăng nhập</label>

          <div class="input-box">
            <span class="icon">👨</span>

            <input
                v-model="form.username"
                type="text"
                placeholder="Nhập tên đăng nhập"
            />
          </div>
        </div>

        <!-- Password -->
        <div class="form-item">
          <label>Mật khẩu</label>

          <div class="input-box">
            <span class="icon">🔒</span>

            <input
                :type="showPassword ? 'text' : 'password'"
                v-model="form.password"
                placeholder="Nhập mật khẩu"
            />

            <span
                class="eye"
                @click="showPassword = !showPassword"
            >
              {{ showPassword ? "🙈" : "👁" }}
            </span>
          </div>
        </div>

        <!-- Confirm -->
        <div class="form-item">
          <label>Xác nhận mật khẩu</label>

          <div class="input-box">
            <span class="icon">🔒</span>

            <input
                :type="showConfirm ? 'text' : 'password'"
                v-model="confirmPassword"
                placeholder="Nhập lại mật khẩu"
            />

            <span
                class="eye"
                @click="showConfirm = !showConfirm"
            >
              {{ showConfirm ? "🙈" : "👁" }}
            </span>
          </div>
        </div>

        <!-- Điều khoản -->
        <label class="agree">
          <input
              type="checkbox"
              v-model="agree"
          />

          <span>
            Tôi đồng ý với Điều khoản sử dụng và Chính sách bảo mật của SAOPhone.
          </span>
        </label>
        <div v-if="error" class="error-message">
          {{ error }}
        </div>
        <!-- Button -->
        <button
            type="submit"
            class="register-btn"
            :disabled="loading"
        >
          {{ loading ? "ĐANG ĐĂNG KÝ..." : "ĐĂNG KÝ NGAY" }}
        </button>

      </form>

      <!-- Divider -->
      <div class="line">
        <span>Hoặc</span>
      </div>

      <!-- Social -->
      <div class="social">

      </div>
      <!-- Chuyển sang đăng nhập -->
      <div class="login">
        Đã có tài khoản?

        <a
            href="#"
            @click.prevent="emit('open-login')"
        >
          Đăng nhập ngay
        </a>
      </div>

    </div>
  </div>
</template>
<script setup>
import { reactive, ref } from "vue";
import { register as registerAPI } from "@/service/AuthService";

// Emit
const emit = defineEmits([
  "close",
  "open-login"
]);

// Form
const form = reactive({
  hoTen: "",
  soDienThoai: "",
  email: "",
  username: "",
  password: ""
});

// Xác nhận mật khẩu
const confirmPassword = ref("");

// Hiện / ẩn mật khẩu
const showPassword = ref(false);
const showConfirm = ref(false);

// Đồng ý điều khoản
const agree = ref(false);

// Loading
const loading = ref(false);

// Lỗi
const error = ref("");

// Đăng ký
const registerUser = async () => {
  console.log("REGISTER CLICK");
  console.log("hoTen =", form.hoTen);
  console.log("soDienThoai =", form.soDienThoai);
  console.log("email =", form.email);
  console.log("username =", form.username);
  console.log("password =", form.password);
  console.log("confirm =", confirmPassword.value);
  console.log("agree =", agree.value);
  error.value = "";

  // Kiểm tra rỗng
  if (
      !form.hoTen ||
      !form.soDienThoai ||
      !form.email ||
      !form.username ||
      !form.password
  ) {
    error.value = "Vui lòng nhập đầy đủ thông tin.";
    return;
  }

  // Email
  const emailRegex =
      /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

  if (!emailRegex.test(form.email)) {
    error.value = "Email không hợp lệ.";
    return;
  }

  // Số điện thoại
  const phoneRegex = /^[0-9]{10}$/;

  if (!phoneRegex.test(form.soDienThoai)) {
    error.value = "Số điện thoại phải gồm đúng 10 số.";
    return;
  }

  // Username
  if (form.username.length < 4) {
    error.value = "Tên đăng nhập phải từ 4 ký tự.";
    return;
  }

  // Password
  if (form.password.length < 6) {
    error.value = "Mật khẩu phải từ 6 ký tự.";
    return;
  }

  // Xác nhận mật khẩu
  if (form.password !== confirmPassword.value) {
    error.value = "Mật khẩu xác nhận không khớp.";
    return;
  }

  // Điều khoản
  if (!agree.value) {
    error.value = "Bạn phải đồng ý điều khoản.";
    return;
  }

  loading.value = true;

  try {
    console.log("CALL API", form);
    const res = await registerAPI({
      hoTen: form.hoTen,
      soDienThoai: form.soDienThoai,
      email: form.email,
      username: form.username,
      password: form.password
    });

    alert(res.data.message);

    // Reset form
    form.hoTen = "";
    form.soDienThoai = "";
    form.email = "";
    form.username = "";
    form.password = "";

    confirmPassword.value = "";

    agree.value = false;

    error.value = "";

    // Chuyển về form đăng nhập
    emit("open-login");

  } catch (e) {

    error.value =
        e.response?.data?.message ||
        "Đăng ký thất bại.";

  } finally {

    loading.value = false;

  }

};
</script>
<style scoped>

/* =======================
   Overlay
======================= */
.error-message{
  margin-bottom:15px;
  padding:12px;
  border-radius:8px;
  background:#ffe5e5;
  color:#d32f2f;
  font-size:14px;
  font-weight:600;
  border:1px solid #ffb3b3;
}
.register-overlay{
  position:fixed;
  inset:0;
  display:flex;
  justify-content:center;
  align-items:center;
  background:rgba(0,0,0,.75);
  backdrop-filter:blur(5px);
  z-index:9999;
  padding:20px;
}

/* =======================
   Card
======================= */

.register-card{
  width:100%;
  max-width:500px;
  max-height:90vh;
  overflow-y:auto;

  background:#181818;

  border-radius:18px;

  padding:30px;

  border:1px solid rgba(255,255,255,.08);

  box-shadow:0 20px 60px rgba(0,0,0,.45);

  animation:show .25s ease;

  position:relative;
}

@keyframes show{

  from{
    opacity:0;
    transform:translateY(15px);
  }

  to{
    opacity:1;
    transform:translateY(0);
  }

}

/* =======================
   Close
======================= */

.close-btn{

  position:absolute;

  right:18px;

  top:18px;

  width:36px;

  height:36px;

  border:none;

  border-radius:50%;

  cursor:pointer;

  background:#2b2b2b;

  color:white;

  transition:.25s;

}

.close-btn:hover{

  background:#facc15;

  color:#000;

}

/* =======================
   Logo
======================= */

.logo{

  text-align:center;

  font-size:34px;

  font-weight:900;

  color:white;

  margin-bottom:8px;

}

.logo span{

  color:#facc15;

}

h2{

  text-align:center;

  color:white;

  font-size:28px;

  font-weight:800;

  margin-bottom:10px;

}

.sub-title{

  text-align:center;

  color:#888;

  font-size:14px;

  margin-bottom:30px;

}

/* =======================
   Form
======================= */

.register-form{

  width:100%;

}

.form-item{

  width:100%;

  margin-bottom:18px;

}

.form-item label{

  display:block;

  color:#ddd;

  margin-bottom:8px;

  font-weight:600;

  font-size:14px;

}

/* =======================
   Input
======================= */

.input-box{

  position:relative;

  width:100%;

}

.input-box input{

  width:100%;

  height:50px;

  border:1px solid #333;

  border-radius:12px;

  background:#111;

  color:white;

  font-size:15px;

  padding-left:45px;

  padding-right:45px;

  box-sizing:border-box;

  transition:.25s;

}

.input-box input::placeholder{

  color:#666;

}

.input-box input:focus{

  outline:none;

  border-color:#facc15;

  box-shadow:0 0 0 3px rgba(250,204,21,.15);

}

/* =======================
   Icon
======================= */

.icon{

  position:absolute;

  left:15px;

  top:50%;

  transform:translateY(-50%);

  color:#888;

}

/* =======================
   Eye
======================= */

.eye{

  position:absolute;

  right:15px;

  top:50%;

  transform:translateY(-50%);

  cursor:pointer;

  user-select:none;

}

/* =======================
   Checkbox
======================= */

.agree{

  display:flex;

  align-items:flex-start;

  gap:10px;

  color:#bbb;

  font-size:14px;

  margin:20px 0;

}

.agree input{

  margin-top:3px;

  accent-color:#facc15;

}

/* =======================
   Button
======================= */

.register-btn{

  width:100%;

  height:50px;

  border:none;

  border-radius:12px;

  background:#facc15;

  color:black;

  font-size:16px;

  font-weight:800;

  cursor:pointer;

  transition:.25s;

}

.register-btn:hover{

  background:#ffd84d;

  transform:translateY(-2px);

}

.register-btn:disabled{

  opacity:.7;

  cursor:not-allowed;

}

/* =======================
   Divider
======================= */

.line{

  display:flex;

  align-items:center;

  margin:28px 0;

}

.line::before,
.line::after{

  content:"";

  flex:1;

  height:1px;

  background:#333;

}

.line span{

  padding:0 15px;

  color:#888;

}

/* =======================
   Social
======================= */

.social{

  display:flex;

  gap:10px;

}

.social button{

  flex:1;

  height:45px;

  border-radius:10px;

  border:1px solid #333;

  background:#222;

  color:white;

  font-weight:700;

  cursor:pointer;

  transition:.25s;

}

.social button:hover{

  background:#333;

}

/* =======================
   Login
======================= */

.login{

  margin-top:25px;

  text-align:center;

  color:#aaa;

}

.login a{

  color:#facc15;

  text-decoration:none;

  font-weight:700;

}

.login a:hover{

  text-decoration:underline;

}

/* =======================
   Responsive
======================= */

@media (max-width:768px){

  .register-card{

    width:100%;

    max-width:100%;

    padding:22px;

    max-height:95vh;

  }

  h2{

    font-size:24px;

  }

  .logo{

    font-size:30px;

  }

}

</style>