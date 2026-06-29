<template>

  <div class="register-overlay">

    <div class="register-card">

      <!-- Đóng -->

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

      <h2>

        Đăng ký tài khoản

      </h2>

      <p class="sub-title">

        Đăng ký thành viên để nhận nhiều ưu đãi, tích điểm và khuyến mãi.

      </p>

      <form
          class="register-form"
          @submit.prevent="register"
      >

        <!-- Họ tên -->

        <div class="form-item">

          <label>

            Họ và tên

          </label>

          <div class="input-box">

                    <span class="icon">

                        👤

                    </span>

            <input

                v-model="form.hoTen"

                placeholder="Nhập họ và tên"

                type="text"

            >

          </div>

        </div>

        <!-- Điện thoại -->

        <div class="form-item">

          <label>

            Số điện thoại

          </label>

          <div class="input-box">

                    <span class="icon">

                        📱

                    </span>

            <input

                v-model="form.soDienThoai"

                placeholder="Nhập số điện thoại"

                type="text"

            >

          </div>

        </div>

        <!-- Email -->

        <div class="form-item">

          <label>

            Email

          </label>

          <div class="input-box">

                    <span class="icon">

                        ✉

                    </span>

            <input

                v-model="form.email"

                placeholder="example@gmail.com"

                type="email"

            >

          </div>

        </div>

        <!-- Username -->

        <div class="form-item">

          <label>

            Tên đăng nhập

          </label>

          <div class="input-box">

                    <span class="icon">

                        👨

                    </span>

            <input

                v-model="form.username"

                placeholder="Tên đăng nhập"

                type="text"

            >

          </div>

        </div>

        <!-- Password -->

        <div class="form-item">

          <label>

            Mật khẩu

          </label>

          <div class="input-box">

                    <span class="icon">

                        🔒

                    </span>

            <input

                :type="showPassword ? 'text':'password'"

                v-model="form.password"

                placeholder="********"

            >

            <span

                class="eye"

                @click="showPassword=!showPassword"

            >

                        {{showPassword ? "🙈":"👁"}}

                    </span>

          </div>

        </div>

        <!-- Confirm -->

        <div class="form-item">

          <label>

            Xác nhận mật khẩu

          </label>

          <div class="input-box">

                    <span class="icon">

                        🔒

                    </span>

            <input

                :type="showConfirm ? 'text':'password'"

                v-model="confirmPassword"

                placeholder="********"

            >

            <span

                class="eye"

                @click="showConfirm=!showConfirm"

            >

                        {{showConfirm ? "🙈":"👁"}}

                    </span>

          </div>

        </div>

        <!-- Agree -->

        <label class="agree">

          <input

              type="checkbox"

              v-model="agree"

          >

          <span>

                    Tôi đồng ý với Điều khoản sử dụng và Chính sách bảo mật.

                </span>

        </label>

        <button

            class="register-btn"

            :disabled="loading"

        >

          {{loading ? "ĐANG ĐĂNG KÝ..." : "ĐĂNG KÝ NGAY"}}

        </button>

      </form>

      <div class="line">

            <span>

                Hoặc

            </span>

      </div>

      <div class="social">

      </div>

      <div class="login">

        Đã có tài khoản?

        <a
            href="#"

            @click.prevent="emit('open-login')"

        >

          Đăng nhập

        </a>

      </div>

    </div>

  </div>

</template>
<script setup>
import { reactive, ref } from "vue";
import { register as registerAPI } from "@/Service/AuthService";
// Emit sự kiện cho component cha
const emit = defineEmits([
  "register",
  "close",
  "open-login",
]);

// Form dữ liệu
const form = reactive({
  hoTen: "",
  soDienThoai: "",
  email: "",
  username: "",
  password: "",
});

// Xác nhận mật khẩu
const confirmPassword = ref("");

// Hiển thị mật khẩu
const showPassword = ref(false);
const showConfirm = ref(false);

// Checkbox điều khoản
const agree = ref(false);

// Loading
const loading = ref(false);

// Lỗi
const error = ref("");

// Đăng ký
const register = async () => {

  error.value = "";

  if (
      !form.hoTen ||
      !form.soDienThoai ||
      !form.email ||
      !form.username ||
      !form.password
  ){
    alert("Vui lòng nhập đầy đủ thông tin.");
    return;
  }

  if(form.password !== confirmPassword.value){
    alert("Mật khẩu xác nhận không khớp.");
    return;
  }

  if(!agree.value){
    alert("Bạn phải đồng ý điều khoản.");
    return;
  }

  loading.value = true;

  try{

    const res = await registerAPI({

      hoTen:form.hoTen,

      soDienThoai:form.soDienThoai,

      email:form.email,

      diaChi:"",

      username:form.username,

      password:form.password

    });

    alert(res.data.message || "Đăng ký thành công");

    form.hoTen="";

    form.soDienThoai="";

    form.email="";

    form.username="";

    form.password="";

    confirmPassword.value="";

    agree.value=false;

    emit("open-login");

  }catch(e){

    alert(
        e.response?.data?.message ||
        e.response?.data ||
        "Đăng ký thất bại."
    );

  }finally{

    loading.value=false;

  }

}
</script>
<style scoped>

/* ===========================
   Overlay
=========================== */

.register-overlay{
  position:fixed;
  inset:0;

  display:flex;
  justify-content:center;
  align-items:center;
  padding:20px;
  background:rgba(0,0,0,.75);
  backdrop-filter:blur(5px);
  z-index:9999;
}

/* ===========================
   Card
=========================== */

.register-card{
  width:100%;
  max-width:520px;
  max-height:90vh;

  overflow-y:auto;

  background:#181818;
  border-radius:18px;

  padding:28px;

  border:1px solid rgba(255,255,255,.08);

  box-shadow:0 20px 60px rgba(0,0,0,.55);

  animation:show .3s ease;

  position:relative;
}

@keyframes show{

  from{
    opacity:0;
    transform:translateY(20px);
  }

  to{
    opacity:1;
    transform:translateY(0);
  }

}

/* ===========================
   Close
=========================== */

.close-btn{

  position:absolute;
  top:18px;
  right:18px;

  width:36px;
  height:36px;

  border:none;
  border-radius:50%;

  background:#2a2a2a;

  color:white;

  cursor:pointer;

  transition:.25s;

}

.close-btn:hover{

  background:#facc15;

  color:black;

}

/* ===========================
   Logo
=========================== */

.logo{

  text-align:center;

  font-size:34px;

  font-weight:900;

  color:white;

  margin-bottom:8px;

  letter-spacing:1px;

}

.logo span{

  color:#facc15;

}

/* ===========================
   Title
=========================== */

h2{

  text-align:center;

  color:white;

  margin:0;

  font-size:28px;

  font-weight:800;

}

.sub-title{

  text-align:center;

  color:#999;

  margin-top:8px;

  margin-bottom:28px;

  font-size:14px;

}

/* ===========================
   Form
=========================== */

.form-item{
  width:100%;
  margin-bottom:18px;
}

.form-item label{

  display:block;

  color:#ddd;

  margin-bottom:8px;

  font-size:14px;

  font-weight:600;

}

/* ===========================
   Input
=========================== */

.input-box{
  width:100%;
  position:relative;
}

.input-box input{

  width:100%;
  height:48px;
  padding:0 45px;
  background:#121212;
  border:1px solid #333;
  border-radius:12px;
  color:#fff;
  font-size:15px;

  box-sizing:border-box;

}

.input-box input::placeholder{

  color:#666;

}

.input-box input:focus{

  outline:none;

  border-color:#facc15;

  box-shadow:0 0 0 3px rgba(250,204,21,.15);

}

/* ===========================
   Icon
=========================== */

.icon{

  position:absolute;

  left:15px;

  top:50%;

  transform:translateY(-50%);

  font-size:18px;

  color:#888;

}

/* ===========================
   Eye
=========================== */

.eye{

  position:absolute;

  right:16px;

  top:50%;

  transform:translateY(-50%);

  cursor:pointer;

  color:#888;

  transition:.25s;

}

.eye:hover{

  color:#facc15;

}

/* ===========================
   Checkbox
=========================== */

.agree{

  display:flex;

  align-items:flex-start;

  gap:10px;

  width:100%;

  margin:18px 0;

  color:#bbb;

  font-size:14px;

}

.agree input{

  width:18px;

  height:18px;

  accent-color:#facc15;

}

/* ===========================
   Button
=========================== */

.register-btn{

  width:100%;

  height:48px;

  border:none;

  border-radius:12px;

  background:#facc15;

  color:#000;

  font-size:17px;

  font-weight:700;

}

.register-btn:hover{

  background:#ffd84d;

  transform:translateY(-2px);

  box-shadow:0 8px 18px rgba(250,204,21,.3);

}

.register-btn:disabled{

  opacity:.7;

  cursor:not-allowed;

}

/* ===========================
   Divider
=========================== */

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

  color:#777;

  font-size:13px;

}

/* ===========================
   Social
=========================== */

.social{

  display:flex;

  gap:12px;

}

.social button{

  flex:1;

  height:46px;

  border-radius:10px;

  border:1px solid #333;

  background:#222;

  color:white;

  font-weight:700;

  cursor:pointer;

  transition:.25s;

}

.social button:hover{

  background:#2d2d2d;

}

.google:hover{

  border-color:#db4437;

}

.facebook:hover{

  border-color:#1877f2;

}

/* ===========================
   Login
=========================== */

.login{

  margin-top:25px;

  text-align:center;

  color:#aaa;

  font-size:14px;

}

.login a{

  color:#facc15;

  text-decoration:none;

  font-weight:700;

}

.login a:hover{

  text-decoration:underline;

}

/* ===========================
   Mobile
=========================== */

@media (max-width:768px){

  .register-card{

    width:95%;

    padding:22px;

    max-height:95vh;

  }

}

  .logo{

    font-size:28px;

  }

  h2{

    font-size:24px;

  }

  .sub-title{

    font-size:13px;

  }

  .input-box input{

    height:45px;

  }

  .register-btn{

    height:46px;

  }


</style>