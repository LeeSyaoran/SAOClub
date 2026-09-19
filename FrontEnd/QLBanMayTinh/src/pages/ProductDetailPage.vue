<template>
  <div
    style="
      background: var(--bg-page);
      color: var(--text-primary);
      font-family: 'Nunito Sans', 'Segoe UI', sans-serif;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    "
  >
    <!-- ── NavBar + Top banner giống trang chủ ── -->
    <NavBar
      :cart-count="cartCount"
      :user="auth && auth.isAdmin ? null : (auth && auth.user)"
      @toggle-cart="onToggleCart"
      @search="onSearch"
      @open-admin="goAdmin"
      @open-account="goAccount"
      @open-login="openLogin"
      @logout="onLogout"
      @select-category="onSelectCategory"
    />

    <!-- Nội dung trang — cuộn độc lập dưới navbar -->
    <main class="flex-grow-1" style="padding-top: 16px; padding-bottom: 48px;">
      <div class="container-xl">
        <!-- ── Breadcrumb ── -->
        <nav
          class="d-flex align-items-center gap-2 mb-3 flex-wrap"
          style="font-size: 13px;"
          aria-label="breadcrumb"
        >
          <router-link
            to="/"
            class="d-inline-flex align-items-center gap-1 text-decoration-none"
            style="color: var(--text-secondary);"
          >
            <Home :size="14" />
            <span>{{ t("productDetail.breadcrumbHome") }}</span>
          </router-link>
          <span style="color: var(--text-muted);">/</span>
          <span style="color: var(--text-secondary);">{{ t("nav.laptop") }}</span>
          <span v-if="product" style="color: var(--text-muted);">/</span>
          <span v-if="product" style="color: var(--text-secondary);">{{ product.tenThuongHieu }}</span>
          <span v-if="product" style="color: var(--text-muted);">/</span>
          <span
            v-if="product"
            class="text-truncate"
            style="max-width: 360px; color: var(--text-primary);"
          >{{ product.tenSanPham }}</span>
        </nav>

        <!-- ── Không tìm thấy sản phẩm ── -->
        <div
          v-if="!loading && !product"
          class="text-center py-5 rounded-3"
          style="background: var(--bg-card); border: 1px solid var(--border-color);"
        >
          <PackageX :size="56" color="var(--text-muted)" class="mb-3" />
          <h2 class="fw-bold mb-2" style="color: var(--text-heading);">
            {{ t("productDetail.notFound") }}
          </h2>
          <p style="color: var(--text-secondary);">
            {{ t("productDetail.notFoundDesc") }}
          </p>
          <router-link
            to="/"
            class="btn fw-bold mt-2"
            style="background: var(--accent); color: var(--accent-text); border: none;"
          >
            {{ t("productDetail.backHome") }}
          </router-link>
        </div>

        <!-- ── Loading ── -->
        <div v-else-if="loading" class="text-center py-5">
          <div class="spinner-border" role="status" style="color: var(--accent);">
            <span class="visually-hidden">{{ t('common.loading') }}</span>
          </div>
        </div>

        <!-- ── Layout 2 cột chính ── -->
        <div v-else class="row g-3">
          <!-- ════════════ CỘT TRÁI: tên + ảnh + cấu hình ════════════ -->
          <div class="col-12 col-lg-7">
            <!-- Tên sản phẩm -->
            <h1
              class="fw-black mb-2"
              style="font-size: 1.5rem; line-height: 1.3; color: var(--text-heading);"
            >
              {{ activeVariant.tenSanPham }}
            </h1>
            <p
              class="mb-3 small"
              style="color: var(--text-secondary);"
            >
              {{ activeVariant.tenThuongHieu }} · {{ activeVariant.tenDanhMuc }}
            </p>

            <!-- Action bar: yêu thích / hỏi đáp / thông số / so sánh -->
            <div
              class="d-flex flex-wrap align-items-center gap-3 mb-3 pb-3"
              style="border-bottom: 1px solid var(--border-color-soft);"
            >
              <button
                type="button"
                class="btn btn-link p-0 d-inline-flex align-items-center gap-1 fw-semibold text-decoration-none"
                style="font-size: 13px; color: var(--accent-fg);"
                @click="$emit('toggle-wishlist', activeVariant)"
              >
                <Heart
                  :size="16"
                  :fill="isCurrentWishlisted ? 'currentColor' : 'none'"
                />
                <span>{{ t("wishlist.add") }}</span>
              </button>

              <button
                type="button"
                class="btn btn-link p-0 d-inline-flex align-items-center gap-1 fw-semibold text-decoration-none"
                style="font-size: 13px; color: var(--text-secondary);"
              >
                <MessageCircle :size="16" />
                <span>{{ t("productDetail.qna") || "Hỏi đáp" }}</span>
              </button>

              <button
                type="button"
                class="btn btn-link p-0 d-inline-flex align-items-center gap-1 fw-semibold text-decoration-none"
                style="font-size: 13px; color: var(--text-secondary);"
                @click="scrollToSpecs"
              >
                <SlidersHorizontal :size="16" />
                <span>{{ t("productDetail.specShort") }}</span>
              </button>

              <button
                type="button"
                class="btn btn-link p-0 d-inline-flex align-items-center gap-1 fw-semibold text-decoration-none"
                style="font-size: 13px; color: var(--text-secondary);"
                @click="$emit('open-product', activeVariant)"
              >
                <GitCompare :size="16" />
                <span>{{ t("productCompare.add") }}</span>
              </button>
            </div>

            <!-- Banner highlight — gradient pink-orange như trong ảnh tham khảo -->
            <div
              class="rounded-3 overflow-hidden mb-3"
              style="
                background: linear-gradient(135deg, #ec4899 0%, #f97316 100%);
                position: relative;
                min-height: 280px;
              "
            >
              <div class="row g-0 h-100 align-items-stretch">
                <!-- Ảnh sản phẩm lớn -->
                <div
                  class="col-12 col-md-5 d-flex align-items-center justify-content-center"
                  style="background: rgba(255,255,255,0.96); padding: 20px; min-height: 280px;"
                >
                  <img
                    v-if="displayedImage"
                    :src="displayedImage"
                    :alt="activeVariant.tenSanPham"
                    style="max-width: 100%; max-height: 240px; object-fit: contain;"
                  />
                  <Laptop v-else :size="96" color="var(--text-muted)" />
                </div>
                <!-- Tính năng nổi bật -->
                <div class="col-12 col-md-7 p-4 d-flex flex-column justify-content-center">
                  <h3
                    class="fw-black mb-3 text-uppercase text-white"
                    style="font-size: 1.05rem; letter-spacing: 0.08em;"
                  >
                    {{ t("productDetail.highlights") }}
                  </h3>
                  <ul class="m-0 ps-3" style="color: #fff; line-height: 1.7; font-size: 13px;">
                    <li v-for="(h, i) in highlights" :key="i" class="mb-2">
                      {{ h }}
                    </li>
                  </ul>
                </div>
              </div>
            </div>

            <!-- Gallery thumbnails -->
            <div
              v-if="galleryImages.length > 1"
              class="d-flex align-items-center gap-2 mb-4"
              style="overflow-x: auto; padding-bottom: 4px;"
            >
              <button
                type="button"
                class="btn btn-sm btn-outline-secondary rounded-circle flex-shrink-0 d-flex align-items-center justify-content-center p-0"
                style="width: 32px; height: 32px;"
                @click="activeImageIndex = Math.max(0, activeImageIndex - 1)"
              >
                <ChevronLeft :size="14" />
              </button>
              <div class="d-flex gap-2 flex-grow-1" style="overflow-x: auto;">
                <button
                  v-for="(url, i) in galleryImages"
                  :key="i"
                  type="button"
                  class="p-0 flex-shrink-0 rounded-2 overflow-hidden"
                  :style="`width: 64px; height: 64px; background: var(--bg-card); border: ${activeImageIndex === i ? '2px solid var(--accent)' : '1px solid var(--border-color)'}; cursor: pointer;`"
                  @click="activeImageIndex = i"
                >
                  <img
                    :src="url"
                    alt=""
                    style="width: 100%; height: 100%; object-fit: cover;"
                  />
                </button>
              </div>
              <button
                type="button"
                class="btn btn-sm btn-outline-secondary rounded-circle flex-shrink-0 d-flex align-items-center justify-content-center p-0"
                style="width: 32px; height: 32px;"
                @click="activeImageIndex = Math.min(galleryImages.length - 1, activeImageIndex + 1)"
              >
                <ChevronRight :size="14" />
              </button>
            </div>

            <!-- ════════════ Thông số kỹ thuật ════════════ -->
            <div id="specs-section" class="mb-4">
              <div class="d-flex justify-content-between align-items-center mb-3">
                <h2
                  class="fw-bold mb-0"
                  style="font-size: 1.05rem; color: var(--text-heading);"
                >
                  {{ t("productDetail.configuration") }}
                </h2>
                <a
                  href="#"
                  class="text-decoration-none small fw-semibold"
                  style="color: var(--accent-fg);"
                  @click.prevent
                >
                  {{ t("productDetail.readMore") }} ›
                </a>
              </div>
              <div
                class="rounded-3 overflow-hidden"
                style="background: var(--bg-card); border: 1px solid var(--border-color);"
              >
                <table class="w-100 mb-0" style="border-collapse: collapse;">
                  <tbody>
                    <tr
                      v-for="s in allSpecs"
                      :key="s.label"
                      style="border-top: 1px solid var(--border-color-soft);"
                    >
                      <td
                        style="
                          width: 38%;
                          padding: 12px 16px;
                          font-size: 13px;
                          white-space: nowrap;
                          background: var(--bg-card-alt);
                          color: var(--text-secondary);
                          font-weight: 600;
                        "
                      >
                        {{ s.label }}
                      </td>
                      <td
                        style="
                          padding: 12px 16px;
                          font-size: 13px;
                          background: var(--bg-card);
                          color: var(--text-primary);
                        "
                      >
                        {{ s.value }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- ════════════ Sản phẩm gợi ý ════════════ -->
            <div v-if="related.length > 0">
              <h2
                class="fw-bold mb-3"
                style="font-size: 1.05rem; color: var(--text-heading);"
              >
                {{ t("productDetail.relatedProducts") }}
              </h2>
              <div
                class="d-flex gap-3 pb-2"
                style="overflow-x: auto; scrollbar-width: thin;"
              >
                <article
                  v-for="p in related"
                  :key="p.sanPhamId"
                  class="flex-shrink-0 rounded-3"
                  style="
                    width: 170px;
                    background: var(--bg-card);
                    border: 1px solid var(--border-color);
                    cursor: pointer;
                    transition: transform 0.15s, box-shadow 0.15s;
                    overflow: hidden;
                  "
                  @mouseenter="(e) => { e.currentTarget.style.transform = 'translateY(-3px)'; e.currentTarget.style.boxShadow = 'var(--shadow-lg)'; }"
                  @mouseleave="(e) => { e.currentTarget.style.transform = ''; e.currentTarget.style.boxShadow = ''; }"
                  @click="goToProduct(p)"
                >
                  <div
                    class="d-flex align-items-center justify-content-center"
                    style="height: 110px; background: var(--bg-card-inset); padding: 8px;"
                  >
                    <img
                      v-if="p.hinhAnhChinh"
                      :src="p.hinhAnhChinh"
                      :alt="p.tenSanPham"
                      style="max-width: 100%; max-height: 90px; object-fit: contain;"
                    />
                    <Laptop v-else :size="40" color="var(--text-muted)" />
                  </div>
                  <div class="p-2">
                    <p
                      class="fw-semibold mb-1"
                      style="
                        font-size: 11px;
                        height: 30px;
                        overflow: hidden;
                        display: -webkit-box;
                        -webkit-line-clamp: 2;
                        line-clamp: 2;
                        -webkit-box-orient: vertical;
                        color: var(--text-primary);
                      "
                    >
                      {{ p.tenSanPham }}
                    </p>
                    <p class="fw-black mb-0" style="font-size: 12px; color: var(--accent-fg);">
                      {{ formatPrice(p.giaBan) }}
                    </p>
                  </div>
                </article>
              </div>
            </div>
          </div>

          <!-- ════════════ CỘT PHẢI: sticky ════════════ -->
          <div class="col-12 col-lg-5">
            <div class="position-sticky" style="top: 80px;">
              <!-- Giá -->
              <div class="d-flex align-items-baseline gap-2 mb-3">
                <span
                  class="fw-black"
                  style="font-size: 1.7rem; color: var(--accent-fg);"
                >
                  {{ formatPrice(activeVariant.giaBan) }}
                </span>
                <span class="small" style="color: var(--text-secondary);">
                  {{ t("productDetail.freeShipping") }}
                </span>
              </div>

              <!-- Phiên bản (cấu hình) -->
              <div v-if="configs.length > 1" class="mb-3">
                <div
                  class="fw-semibold mb-2"
                  style="
                    font-size: 0.72rem;
                    text-transform: uppercase;
                    letter-spacing: 0.06em;
                    color: var(--text-secondary);
                  "
                >
                  {{ t("productDetail.versions", { count: configs.length }) }}
                </div>
                <div class="d-flex flex-wrap gap-2">
                  <button
                    v-for="v in configs"
                    :key="configKey(v)"
                    class="btn btn-sm d-flex flex-column align-items-start text-start px-3 py-2"
                    style="border-radius: 10px; min-width: 140px; transition: all 0.15s;"
                    :style="activeConfigKey === configKey(v)
                      ? 'background: rgba(244,63,94,0.12); border: 1.5px solid var(--accent); color: var(--accent-fg);'
                      : 'background: var(--bg-input); border: 1.5px solid var(--border-color-strong); color: var(--text-secondary);'"
                    @click="selectConfig(v)"
                  >
                    <span class="fw-semibold" style="font-size: 11px; line-height: 1.5;">
                      {{ configLabel(v).line1 }}
                    </span>
                    <span v-if="configLabel(v).line2" style="font-size: 10px; opacity: 0.75;">
                      {{ configLabel(v).line2 }}
                    </span>
                  </button>
                </div>
              </div>

              <!-- Màu sắc — dạng chip với ảnh nhỏ -->
              <div v-if="colorsForConfig.length > 0" class="mb-3">
                <div
                  class="fw-semibold mb-2"
                  style="
                    font-size: 0.72rem;
                    text-transform: uppercase;
                    letter-spacing: 0.06em;
                    color: var(--text-secondary);
                  "
                >
                  {{ t("productDetail.colorHeading") }}
                </div>
                <div class="d-flex flex-wrap gap-2">
                  <button
                    v-for="v in colorsForConfig"
                    :key="v.bienTheId"
                    type="button"
                    class="d-flex align-items-center gap-2 px-2 py-2 rounded-2"
                    style="
                      min-width: 100px;
                      transition: all 0.15s;
                      background: var(--bg-card);
                    "
                    :style="activeColor === (v.mauSac || '')
                      ? 'border: 2px solid var(--accent); background: rgba(244,63,94,0.08);'
                      : 'border: 1.5px solid var(--border-color-strong);'"
                    @click="selectColor(v)"
                  >
                    <img
                      v-if="v.hinhAnhBienThe || v.hinhAnhChinh"
                      :src="v.hinhAnhBienThe || v.hinhAnhChinh"
                      :alt="v.mauSac"
                      style="
                        width: 36px;
                        height: 36px;
                        object-fit: contain;
                        background: #fff;
                        border-radius: 4px;
                        padding: 2px;
                      "
                    />
                    <span
                      v-else
                      class="rounded-circle flex-shrink-0"
                      :style="`width: 18px; height: 18px; background: ${colorDot(v.mauSac)}; border: 1.5px solid #666;`"
                    ></span>
                    <span
                      v-if="activeColor === (v.mauSac || '')"
                      class="d-inline-flex align-items-center justify-content-center rounded-circle"
                      style="
                        width: 14px;
                        height: 14px;
                        background: var(--accent);
                        position: absolute;
                        transform: translate(20px, 10px);
                      "
                    >
                      <Check :size="10" color="#fff" />
                    </span>
                    <span class="fw-semibold small" style="color: var(--text-primary);">
                      {{ v.mauSac }}
                    </span>
                  </button>
                </div>
              </div>

              <!-- ════════════ Box khuyến mãi ════════════ -->
              <div
                class="rounded-3 mb-3 overflow-hidden"
                style="
                  background: var(--bg-card);
                  border: 2px solid var(--accent);
                "
              >
                <div
                  class="d-flex align-items-center justify-content-between px-3 py-2"
                  style="
                    background: linear-gradient(135deg, var(--accent-2), var(--accent));
                    color: #fff;
                  "
                >
                  <span class="fw-bold d-inline-flex align-items-center gap-2" style="font-size: 14px;">
                    <Gift :size="16" />
                    {{ t("productDetail.promotionHeading") }}
                  </span>
                  <a
                    href="#"
                    class="text-decoration-none small fw-semibold"
                    style="color: #fff;"
                    @click.prevent
                  >
                    {{ t("productDetail.viewAllVouchers") }} ›
                  </a>
                </div>
                <div class="p-3">
                  <div
                    v-for="(v, i) in vouchers"
                    :key="i"
                    class="d-flex align-items-center gap-3 p-2 mb-2 rounded-2"
                    style="
                      background: rgba(244,63,94,0.06);
                      border: 1px dashed var(--accent);
                    "
                  >
                    <div
                      class="flex-shrink-0 px-2 py-1 text-center"
                      style="
                        background: var(--accent);
                        color: #fff;
                        font-weight: 900;
                        font-size: 10px;
                        border-radius: 4px;
                        line-height: 1.1;
                        min-width: 56px;
                      "
                    >
                      <div style="font-size: 9px; opacity: 0.9;">{{ voucherBadgeText(v.percent) }}</div>
                      <div style="font-size: 14px;">{{ v.percent }}</div>
                    </div>
                    <div class="flex-grow-1" style="min-width: 0;">
                      <div class="fw-semibold small" style="color: var(--text-primary);">
                        {{ t("productDetail.voucherTitle", { percent: v.percent }) }}
                      </div>
                      <div
                        class="text-truncate"
                        style="font-size: 11px; color: var(--text-secondary);"
                      >
                        {{ v.desc }}
                      </div>
                    </div>
                    <button
                      type="button"
                      class="btn btn-sm fw-bold flex-shrink-0"
                      style="
                        background: var(--accent);
                        color: #fff;
                        border: none;
                        font-size: 10px;
                        padding: 4px 10px;
                      "
                    >
                      {{ t("productDetail.voucherAction") }}
                    </button>
                  </div>

                  <!-- Lợi ích đi kèm -->
                  <div
                    class="d-flex align-items-start gap-2 mt-2"
                    style="font-size: 12px; color: var(--text-primary);"
                  >
                    <span style="color: var(--accent-fg); font-weight: 900;">1</span>
                    <span>{{ t("productDetail.benefit1") }}</span>
                  </div>
                  <div
                    class="d-flex align-items-start gap-2 mt-1"
                    style="font-size: 12px; color: var(--text-primary);"
                  >
                    <span style="color: var(--accent-fg); font-weight: 900;">2</span>
                    <span>{{ t("productDetail.benefit2") }}</span>
                  </div>
                  <div
                    class="d-flex align-items-start gap-2 mt-1"
                    style="font-size: 12px; color: var(--text-primary);"
                  >
                    <span style="color: var(--accent-fg); font-weight: 900;">3</span>
                    <span>{{ t("productDetail.benefit3") }}</span>
                  </div>
                </div>
              </div>

              <!-- ════════════ Shipping info ════════════ -->
              <div
                class="d-flex align-items-start gap-2 mb-3 p-2 rounded-2"
                style="background: var(--bg-card); border: 1px solid var(--border-color);"
              >
                <Truck :size="20" color="var(--accent-fg)" class="flex-shrink-0" />
                <div class="flex-grow-1">
                  <div class="fw-bold small" style="color: var(--text-primary);">
                    {{ t("productDetail.shippingInfo") }}
                  </div>
                  <div
                    class="d-inline-flex align-items-center gap-1 mt-1 px-2 py-0.5 rounded-2 fw-bold"
                    style="
                      background: var(--accent);
                      color: #fff;
                      font-size: 10px;
                    "
                  >
                    <Zap :size="10" />
                    {{ t("productDetail.shippingFast") }}
                  </div>
                  <span class="ms-2 small" style="color: var(--text-secondary);">
                    {{ t("productDetail.shippingNote", { city: "Hà Nội" }) }}
                  </span>
                  <div class="mt-1">
                    <a
                      href="#"
                      class="text-decoration-none small fw-semibold"
                      style="color: var(--accent-fg);"
                      @click.prevent
                    >
                      📍 {{ t("productDetail.changeAddress") }}
                    </a>
                  </div>
                </div>
              </div>

              <!-- ════════════ 3 nút hành động ════════════ -->
              <div class="row g-2 mb-3">
                <div class="col-4">
                  <button
                    type="button"
                    class="w-100 h-100 fw-bold rounded-2"
                    style="
                      background: transparent;
                      border: 1.5px solid var(--accent);
                      color: var(--accent-fg);
                      padding: 10px 4px;
                      font-size: 12px;
                    "
                  >
                    <CreditCard :size="16" class="mb-1" />
                    <div>{{ t("productDetail.installment") }}</div>
                    <div style="font-size: 9px; font-weight: 400; opacity: 0.8;">
                      {{ t("productDetail.installmentNote") }}
                    </div>
                  </button>
                </div>
                <div class="col-4">
                  <button
                    type="button"
                    class="w-100 h-100 fw-bold rounded-2"
                    style="
                      background: var(--accent);
                      border: 1.5px solid var(--accent);
                      color: #fff;
                      padding: 10px 4px;
                      font-size: 13px;
                    "
                    :disabled="isOutOfStock"
                    @click="onBuyNow"
                  >
                    <Zap :size="14" class="mb-1" />
                    <div>{{ t("productDetail.buyNow") }}</div>
                    <div style="font-size: 9px; font-weight: 400; opacity: 0.9;">
                      {{ t("productDetail.buyNowNote") }}
                    </div>
                  </button>
                </div>
                <div class="col-4">
                  <button
                    type="button"
                    class="w-100 h-100 fw-bold rounded-2"
                    style="
                      background: transparent;
                      border: 1.5px solid var(--accent);
                      color: var(--accent-fg);
                      padding: 10px 4px;
                      font-size: 12px;
                    "
                    :disabled="isOutOfStock"
                    @click="$emit('add-to-cart', activeVariant)"
                  >
                    <ShoppingCart :size="16" class="mb-1" />
                    <div>{{ t("productDetail.addToCartShort") }}</div>
                  </button>
                </div>
              </div>

              <!-- Thanh toán -->
              <div
                class="rounded-2 px-3 py-2 d-flex align-items-center justify-content-between"
                style="
                  background: var(--accent);
                  color: #fff;
                "
              >
                <div class="fw-black" style="font-size: 16px; letter-spacing: 0.04em;">
                  {{ t("productDetail.installment") }} ·
                  <span style="font-size: 11px; opacity: 0.95;">
                    {{ t("productDetail.installmentNote") }}
                  </span>
                </div>
                <div class="d-flex align-items-center gap-2">
                  <CreditCard :size="22" color="#fff" />
                  <span
                    style="
                      font-size: 10px;
                      font-weight: 900;
                      background: #fff;
                      color: var(--accent);
                      padding: 2px 6px;
                      border-radius: 4px;
                    "
                  >VISA</span>
                  <span
                    style="
                      font-size: 10px;
                      font-weight: 900;
                      background: #fff;
                      color: var(--accent);
                      padding: 2px 6px;
                      border-radius: 4px;
                    "
                  >JCB</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- ── Footer giống trang chủ ── -->
    <AppFooter @open-register="onOpenRegister" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, inject } from "vue";
import { useRoute, useRouter } from "vue-router";
import { t } from "../i18n/index.js";
import { formatPrice as formatPriceRaw } from "../utils/formatPrice.js";
import { configKey, configLabel, colorDot } from "../utils/productGrouping.js";
import * as SanPhamService from "../services/SanPhamService.js";
import NavBar from "../components/layout/NavBar.vue";
import AppFooter from "../components/layout/Footer.vue";
import {
  Home,
  Heart,
  MessageCircle,
  SlidersHorizontal,
  GitCompare,
  Laptop,
  ChevronLeft,
  ChevronRight,
  Gift,
  Truck,
  Zap,
  CreditCard,
  ShoppingCart,
  PackageX,
  Check,
} from "@lucide/vue";

const route = useRoute();
const router = useRouter();

defineEmits(["add-to-cart", "toggle-wishlist", "open-product"]);

const {
  products,
  cartCount,
  auth,
  wishlistIds,
} = inject("appState");

const {
  addToCart,
  openLogin,
  openRegister,
  onLogout,
  showToast,
} = inject("appActions");

// ── Tải sản phẩm theo id trên URL ───────────────────────────────────────────────
const loading = ref(true);

const fetchProduct = async () => {
  loading.value = true;
  try {
    await Promise.all([
      // Đảm bảo store đã có dữ liệu — nếu rỗng (vào thẳng URL) thì fetch trước.
      products.value.length === 0
        ? SanPhamService.getAll().catch(() => [])
        : Promise.resolve(),
    ]);
  } finally {
    loading.value = false;
  }
};

// ── Tìm sản phẩm theo sanPhamId trong URL ───────────────────────────────────────
const product = computed(() => {
  const id = Number(route.params.id);
  if (!id) return null;
  // Tìm variant đầu tiên có sanPhamId khớp
  return products.value.find((p) => p.sanPhamId === id) ?? null;
});

const variants = computed(() => {
  if (!product.value) return [];
  return products.value.filter((p) => p.sanPhamId === product.value.sanPhamId);
});

const activeConfigKey = ref("");
const activeColor = ref("");

watch(
  product,
  (p) => {
    if (!p) return;
    activeConfigKey.value = configKey(p);
    activeColor.value = p.mauSac ?? "";
  },
  { immediate: true }
);

watch(() => route.params.id, () => fetchProduct(), { immediate: true });

// ── Configurations & colors ──────────────────────────────────────────────────────
const configs = computed(() => {
  const seen = new Set();
  return variants.value.filter((v) => {
    const k = configKey(v);
    if (seen.has(k)) return false;
    seen.add(k);
    return true;
  });
});

const colorsForConfig = computed(() => {
  const seen = new Set();
  return variants.value
    .filter((v) => configKey(v) === activeConfigKey.value)
    .filter((v) => {
      const c = v.mauSac ?? "";
      if (seen.has(c)) return false;
      seen.add(c);
      return true;
    });
});

const activeVariant = computed(
  () =>
    variants.value.find(
      (v) =>
        configKey(v) === activeConfigKey.value &&
        (v.mauSac ?? "") === activeColor.value
    ) ?? product.value
);

const selectConfig = (v) => {
  activeConfigKey.value = configKey(v);
  const available = variants.value.filter(
    (vv) => configKey(vv) === activeConfigKey.value
  );
  if (!available.find((vv) => (vv.mauSac ?? "") === activeColor.value)) {
    activeColor.value = available[0]?.mauSac ?? "";
  }
};

const selectColor = (v) => {
  activeColor.value = v.mauSac ?? "";
};

const isCurrentWishlisted = computed(() =>
  activeVariant.value ? wishlistIds.value.has(activeVariant.value.bienTheId) : false
);

// ── Stock ───────────────────────────────────────────────────────────────────────
const LOW_STOCK_THRESHOLD = 5;
const stockBadgeClass = computed(() => {
  if (!activeVariant.value) return "bg-secondary";
  if (
    activeVariant.value.trangThai !== "active" ||
    (activeVariant.value.soLuongTon ?? 0) <= 0
  )
    return "bg-secondary";
  if (activeVariant.value.soLuongTon <= LOW_STOCK_THRESHOLD)
    return "bg-warning text-dark";
  return "bg-success";
});
const isOutOfStock = computed(() => stockBadgeClass.value === "bg-secondary");

// ── Gallery ─────────────────────────────────────────────────────────────────────
const galleryExtra = ref([]);
const activeImageIndex = ref(0);

const loadGallery = async (sanPhamId) => {
  activeImageIndex.value = 0;
  try {
    galleryExtra.value = await SanPhamService.getHinhAnh(sanPhamId);
  } catch {
    galleryExtra.value = [];
  }
};

watch(
  () => product.value?.sanPhamId,
  (id) => {
    if (id) loadGallery(id);
  },
  { immediate: true }
);

const galleryImages = computed(() => {
  if (!activeVariant.value) return [];
  const main = activeVariant.value.hinhAnhChinh;
  const list = main
    ? [main, ...galleryExtra.value.filter((u) => u !== main)]
    : galleryExtra.value;
  return list;
});

const displayedImage = computed(
  () =>
    galleryImages.value[activeImageIndex.value] ??
    activeVariant.value?.hinhAnhChinh ??
    null
);

// ── Highlight bullets — tự sinh từ cấu hình ────────────────────────────────────
const highlights = computed(() => {
  const v = activeVariant.value;
  if (!v) return [];
  const items = [];
  if (v.cpu)
    items.push(
      `${v.tenSanPham} trang bị bộ vi xử lý ${v.cpu}, mang lại hiệu suất mạnh mẽ cho các tác vụ hàng ngày và làm việc đa nhiệm.`
    );
  if (v.ram && v.oCung)
    items.push(
      `Với RAM ${v.ram} và ổ cứng ${v.oCung}, máy cho phép lưu trữ lớn và truy xuất nhanh, đáp ứng tốt nhu cầu sử dụng.`
    );
  if (v.kichThuocManHinh)
    items.push(
      `Màn hình ${v.kichThuocManHinh} với độ phân giải sắc nét, cho trải nghiệm hình ảnh sống động và chân thực.`
    );
  if (v.gpu)
    items.push(
      `Card đồ họa ${v.gpu} hỗ trợ tốt cho công việc đồ họa, chỉnh sửa video và giải trí.`
    );
  if (v.pin)
    items.push(
      `Pin dung lượng ${v.pin} cho phép sử dụng liên tục nhiều giờ, phù hợp với người dùng thường xuyên di chuyển.`
    );
  return items.slice(0, 4);
});

// ── Vouchers mẫu (placeholder) ──────────────────────────────────────────────────
const vouchers = computed(() => [
  {
    percent: "6%",
    desc: t("productDetail.voucherDesc", {
      amount: "500K",
      min: "Laptop",
      date: "30/09/2026",
    }),
  },
  {
    percent: "6%",
    desc: "Áp dụng toàn bộ Laptop (trừ Macbook) · 30/09/2026",
  },
]);

const voucherBadgeText = (percent) => {
  const map = { "6%": "Giảm", "5%": "Giảm", "10%": "Giảm" };
  return map[percent] ?? "Giảm";
};

// ── Tất cả thông số (một bảng lớn như ảnh tham khảo) ───────────────────────────
const allSpecs = computed(() => {
  const v = activeVariant.value;
  if (!v) return [];
  const s = t("productDetail.specs");
  const rows = [
    { label: s.gpu, value: v.gpu },
    { label: s.ram, value: v.ram },
    { label: s.storage, value: v.oCung },
    { label: s.cpu, value: v.cpu },
    { label: s.screenSize, value: v.kichThuocManHinh },
    { label: s.color, value: v.mauSac },
    { label: s.weight, value: v.trongLuongKg ? `${v.trongLuongKg} kg` : null },
    { label: s.os, value: v.heDieuHanh },
    { label: s.battery, value: v.pin },
    { label: s.warranty, value: v.baoHanhThang ? `${v.baoHanhThang} ${t("productDetail.months")}` : null },
    { label: s.brand, value: v.tenThuongHieu },
    { label: s.category, value: v.tenDanhMuc },
    { label: s.supplier, value: v.tenNhaCungCap },
    { label: s.productType, value: v.loaiSanPham },
  ];
  return rows.filter((r) => r.value);
});

// ── Sản phẩm gợi ý ─────────────────────────────────────────────────────────────
const related = computed(() => {
  if (!product.value) return [];
  const seen = new Set();
  return products.value
    .filter(
      (p) =>
        p.sanPhamId !== product.value.sanPhamId &&
        (p.tenDanhMuc === product.value.tenDanhMuc ||
          p.tenThuongHieu === product.value.tenThuongHieu)
    )
    .filter((p) => {
      if (seen.has(p.sanPhamId)) return false;
      seen.add(p.sanPhamId);
      return true;
    })
    .slice(0, 8);
});

const formatPrice = (v) =>
  v == null ? t("productDetail.contact") : formatPriceRaw(v);

const goToProduct = (p) => {
  router.push({ name: "product-detail", params: { id: p.sanPhamId } });
  window.scrollTo({ top: 0, behavior: "smooth" });
};

const scrollToSpecs = () => {
  const el = document.getElementById("specs-section");
  if (el) el.scrollIntoView({ behavior: "smooth", block: "start" });
};

const onBuyNow = () => {
  showToast("Tính năng mua nhanh đang phát triển", "info");
};

// ── NavBar/Footer wiring ────────────────────────────────────────────────────────
const onToggleCart = () => showToast("Mở giỏ hàng", "info");
const onSearch = (q) => router.push({ path: "/", query: { q } });
const goAdmin = () => router.push("/admin");
const goAccount = () => router.push("/account");
const onSelectCategory = () => router.push("/");
const onOpenRegister = () => openRegister();

onMounted(() => {
  // Đảm bảo body scroll bình thường (không khoá như overlay cũ)
  document.body.style.overflow = "";
});
</script>

<style scoped>
.btn-link:hover {
  text-decoration: underline !important;
}
</style>
