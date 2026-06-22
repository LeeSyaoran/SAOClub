<template>
  <header class="navbar">
    <div class="navbar-brand">
      <div class="brand-mark">SAO</div>
      <div>
        <div class="brand-name">SAOPhone</div>
        <div class="brand-sub">Hệ thống công nghệ Đen & Vàng</div>
      </div>
    </div>
    <nav class="navbar-links">
      <a href="/">Trang chủ</a>
      <a href="#admin">Quản trị</a>
    </nav>
  </header>
</template>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  background: rgba(10, 10, 10, 0.95);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  position: sticky;
  top: 0;
  z-index: 20;
}
.navbar-brand {
  display: flex;
  align-items: center;
  gap: 14px;
}
.brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: #f4c200;
  display: grid;
  place-items: center;
  font-weight: 800;
  color: #111;
}
.brand-name {
  color: #f5f5f5;
  font-weight: 800;
  font-size: 1rem;
}
.brand-sub {
  color: #b0b0b0;
  font-size: 0.8rem;
}
.navbar-links {
  display: flex;
  align-items: center;
  gap: 18px;
}
.navbar-links a {
  color: #f5f5f5;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.2s;
}
.navbar-links a:hover {
  color: #f4c200;
}
</style>
