<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import { AuthStore } from "../../stores/index.js";
import Skeleton from "../common/Skeleton.vue";
import {
  ShieldPlus, ShieldCheck, Star, Clock, ChevronDown, ChevronUp,
  CheckCircle2, Calendar, Package, Tag, Truck, Shield, Gift,
  Sparkles, User as UserIcon, Info, RefreshCw, XCircle, Plus,
} from '@lucide/vue';

const emit = defineEmits(["toast"]);

const auth = AuthStore;

// ── Mock data: danh sách gói bảo hành mở rộng ────────────────────────────
const PLAN_LIST = [
  {
    id: 'warranty12',
    name: 'Gói Bảo Hành Mở Rộng 12 Tháng',
    duration: 12,
    durationLabel: '12 tháng',
    price: 199000,
    originalPrice: 290000,
    discount: 31,
    color: 'blue',
    badge: 'Tiết kiệm 31%',
    features: [
      'Gia hạn bảo hành thêm 12 tháng cho sản phẩm',
      'Bao gồm lỗi phần cứng do nhà sản xuất',
      'Miễn phí công thay thế & linh kiện',
      'Hỗ trợ kỹ thuật ưu tiên 24/7',
      'Áp dụng cho 1 sản phẩm đã mua',
    ],
    icon: ShieldPlus,
    code: 'BH12M',
  },
  {
    id: 'warranty24',
    name: 'Gói Bảo Hành Mở Rộng 24 Tháng',
    duration: 24,
    durationLabel: '24 tháng',
    price: 349000,
    originalPrice: 580000,
    discount: 40,
    color: 'gold',
    badge: 'Tiết kiệm 40%',
    features: [
      'Gia hạn bảo hành thêm 24 tháng cho sản phẩm',
      'Bao gồm tất cả lỗi phần cứng & pin (nếu có)',
      'Miễn phí công thay thế, pin mới (nếu chai > 60%)',
      'Hỗ trợ kỹ thuật ưu tiên 24/7',
      'Tặng 1 lần vệ sinh máy miễn phí',
      'Áp dụng cho 1 sản phẩm đã mua',
    ],
    icon: ShieldCheck,
    code: 'BH24M',
    popular: true,
  },
  {
    id: 'warranty36',
    name: 'Gói Bảo Hành VIP 36 Tháng',
    duration: 36,
    durationLabel: '36 tháng',
    price: 499000,
    originalPrice: 870000,
    discount: 43,
    color: 'platinum',
    badge: 'VIP',
    features: [
      'Gia hạn bảo hành lên tới 36 tháng',
      'Bao gồm rơi vỡ, vào nước (một lần miễn phí)',
      'Miễn phí thay thế linh kiện, pin, màn hình',
      'Hỗ trợ tận nơi (chỉ nội thành HN, HCM)',
      'Máy thay thế trong khi sửa chữa',
      'Tặng 2 lần vệ sinh máy miễn phí',
      'Áp dụng cho 1 sản phẩm đã mua',
    ],
    icon: Sparkles,
    code: 'VIP36M',
  },
];

// ── Tab order products ───────────────────────────────────────────────────
const eligibleProducts = ref([]);
const loadingProducts = ref(false);
const expandedOrderId = ref(null);

const toggleOrder = (id) => {
  expandedOrderId.value = expandedOrderId.value === id ? null : id;
};

// Mock fetch - sau này thay bằng DonHangService
const fetchEligibleProducts = async () => {
  loadingProducts.value = true;
  try {
    await new Promise((r) => setTimeout(r, 700));
    eligibleProducts.value = [
      {
        donHangId: 'WN0304845879',
        ngayDat: '25/08/2026',
        sanPham: [
          {
            chiTietId: 'CT001',
            tenSanPham: 'MÁY QUAY 4K CHỐNG RUNG DJI OSMO POCKET 4',
            hinhAnh: null,
            maSku: 'DJI-OSMO-PKT4-BLK',
            soSerial: 'SN9XP2M3K7L9',
            ngayMua: '25/08/2026',
            ngayHetBh: '25/08/2027',
            sanPhamId: 'DJI-PKT4',
            warrantyRemainingDays: 365,
          },
        ],
      },
      {
        donHangId: 'WN0304610731',
        ngayDat: '22/06/2026',
        sanPham: [
          {
            chiTietId: 'CT002',
            tenSanPham: 'Laptop ASUS Vivobook S14 S3407CA-LY095WS',
            hinhAnh: null,
            maSku: 'ASUS-S14-S3407CA',
            soSerial: 'SN8YZ2A3B7K1',
            ngayMua: '22/06/2026',
            ngayHetBh: '22/06/2027',
            sanPhamId: 'ASUS-S14',
            warrantyRemainingDays: 320,
          },
        ],
      },
    ];
  } catch (e) {
    emit('toast', 'Không tải được sản phẩm', 'error');
  } finally {
    loadingProducts.value = false;
  }
};

// ── Selected product + selected plan ─────────────────────────────────────
const selectedProduct = ref(null);
const selectedPlan = ref(null);
const showRegisterModal = ref(false);

const openRegister = (product, planId) => {
  const plan = PLAN_LIST.find((p) => p.id === planId);
  if (!plan) return;
  selectedProduct.value = product;
  selectedPlan.value = plan;
  showRegisterModal.value = true;
};

const closeRegister = () => {
  showRegisterModal.value = false;
  selectedProduct.value = null;
  selectedPlan.value = null;
};

const sending = ref(false);
const submitRegistration = async () => {
  if (!selectedProduct.value || !selectedPlan.value) return;
  sending.value = true;
  try {
    await new Promise((r) => setTimeout(r, 1500));
    emit('toast',
      `Đăng ký thành công gói ${selectedPlan.value.durationLabel} cho sản phẩm ${selectedProduct.value.tenSanPham}`,
      'success'
    );
    closeRegister();
  } catch (e) {
    emit('toast', e.message || 'Đăng ký thất bại', 'error');
  } finally {
    sending.value = false;
  }
};

const formatPrice = (v) => {
  if (v == null) return '—';
  return new Intl.NumberFormat('vi-VN').format(v) + ' đ';
};

const summary = computed(() => {
  return PLAN_LIST.map((plan) => {
    return {
      ...plan,
      appliedCount: Math.floor(Math.random() * 5), // mock count
    };
  });
});

onMounted(() => {
  fetchEligibleProducts();
});
</script>

<template>
  <div class="warranty-plan-tab">
    <!-- Header banner -->
    <div class="plan-hero">
      <div class="plan-hero-content">
        <div class="plan-hero-icon">
          <ShieldPlus :size="32" />
        </div>
        <div>
          <h3 class="plan-hero-title">Gia hạn bảo hành cho sản phẩm của bạn</h3>
          <p class="plan-hero-desc">
            Mở rộng thời gian bảo hành lên tới 36 tháng. Áp dụng cho sản phẩm SAOPhone trong vòng 30 ngày trước khi hết hạn.
          </p>
        </div>
      </div>
      <div class="plan-hero-stats">
        <div class="hero-stat">
          <div class="hero-stat-value">4.8/5</div>
          <div class="hero-stat-label"><Star :size="11" /> Đánh giá</div>
        </div>
        <div class="hero-stat">
          <div class="hero-stat-value">12k+</div>
          <div class="hero-stat-label"><UserIcon :size="11" /> Khách đã đăng ký</div>
        </div>
      </div>
    </div>

    <!-- Plans Grid -->
    <div class="plan-grid">
      <div
        v-for="plan in PLAN_LIST" :key="plan.id"
        class="plan-card"
        :class="[`plan-card--${plan.color}`, { 'plan-card--popular': plan.popular }]"
      >
        <div v-if="plan.popular" class="plan-popular-badge">Phổ biến nhất</div>
        <div v-if="plan.badge && !plan.popular" class="plan-discount-badge">{{ plan.badge }}</div>

        <div class="plan-card-header">
          <div class="plan-icon" :class="`plan-icon--${plan.color}`">
            <component :is="plan.icon" :size="24" />
          </div>
          <div class="plan-meta">
            <div class="plan-name">{{ plan.name }}</div>
            <div class="plan-duration">
              <Clock :size="12" /> Gia hạn <strong>{{ plan.durationLabel }}</strong>
            </div>
          </div>
        </div>

        <div class="plan-price-block">
          <div class="plan-price-row">
            <span class="plan-price-current">{{ formatPrice(plan.price) }}</span>
            <span v-if="plan.originalPrice" class="plan-price-original">{{ formatPrice(plan.originalPrice) }}</span>
          </div>
          <div v-if="plan.discount" class="plan-discount-text">Tiết kiệm {{ plan.discount }}%</div>
        </div>

        <div class="plan-features">
          <div v-for="(f, i) in plan.features" :key="i" class="plan-feature">
            <CheckCircle2 :size="13" />
            <span>{{ f }}</span>
          </div>
        </div>

        <button class="plan-cta" @click="$emit('select-plan', plan)">
          Chọn gói này
        </button>
      </div>
    </div>

    <!-- Eligible Products -->
    <div class="eligible-section">
      <h4 class="eligible-title">
        <Package :size="16" />
        Sản phẩm có thể đăng ký
        <span class="count-pill">{{ eligibleProducts.reduce((a, o) => a + o.sanPham.length, 0) }}</span>
      </h4>

      <div v-if="loadingProducts" class="loading-state">
        <Skeleton v-for="i in 3" :key="i" width="100%" height="60px" radius="12px" />
      </div>

      <div v-else-if="eligibleProducts.length === 0" class="empty-state-card">
        <Package :size="40" class="empty-icon" />
        <div class="empty-title">Bạn chưa có sản phẩm nào đủ điều kiện</div>
        <div class="empty-desc">Mua hàng trước rồi quay lại đăng ký gói nhé!</div>
      </div>

      <div v-else class="order-group">
        <div
          v-for="order in eligibleProducts" :key="order.donHangId"
          class="order-block"
        >
          <button
            class="order-summary"
            @click="toggleOrder(order.donHangId)"
          >
            <div class="order-info">
              <span class="order-label">Đơn hàng:</span>
              <strong class="order-code">#{{ order.donHangId }}</strong>
              <span class="order-dot">·</span>
              <span class="order-date">{{ order.ngayDat }}</span>
            </div>
            <div class="order-summary-right">
              <span class="product-count">{{ order.sanPham.length }} sản phẩm</span>
              <ChevronDown :size="16" :class="{ 'is-open': expandedOrderId === order.donHangId }" />
            </div>
          </button>

          <Transition name="slide-down">
            <div v-if="expandedOrderId === order.donHangId" class="order-products-list">
              <div
                v-for="p in order.sanPham" :key="p.chiTietId"
                class="product-card"
              >
                <div class="product-thumb">
                  <Package v-if="!p.hinhAnh" :size="32" />
                  <img v-else :src="p.hinhAnh" />
                </div>

                <div class="product-info">
                  <div class="product-name">{{ p.tenSanPham }}</div>
                  <div class="product-meta">
                    <span><strong>SKU:</strong> <code>{{ p.maSku }}</code></span>
                    <span><strong>Serial:</strong> <code>{{ p.soSerial }}</code></span>
                  </div>
                  <div class="product-warranty">
                    <Calendar :size="12" />
                    Bảo hành đến <strong>{{ p.ngayHetBh }}</strong>
                    ({{ p.warrantyRemainingDays }} ngày)
                  </div>
                </div>

                <div class="product-plans">
                  <div class="product-plans-label">
                    <Tag :size="12" />
                    Chọn gói:
                  </div>
                  <div class="plan-buttons">
                    <button
                      v-for="plan in PLAN_LIST" :key="plan.id"
                      class="plan-btn"
                      :class="[`plan-btn--${plan.color}`]"
                      @click="openRegister(p, plan.id)"
                    >
                      <component :is="plan.icon" :size="13" />
                      <span>{{ plan.durationLabel }}</span>
                      <strong>{{ formatPrice(plan.price) }}</strong>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </div>

    <!-- Registration Modal -->
    <Transition name="modal-fade">
      <div v-if="showRegisterModal" class="modal-overlay" @click.self="closeRegister">
        <div class="modal-content">
          <div class="modal-header">
            <div class="modal-title">
              <ShieldPlus :size="22" />
              <div>
                <div class="modal-title-main">Đăng ký gói bảo hành mở rộng</div>
                <div class="modal-title-sub">{{ selectedProduct?.tenSanPham }}</div>
              </div>
            </div>
            <button class="modal-close" @click="closeRegister">
              <XCircle :size="18" />
            </button>
          </div>

          <div class="modal-body">
            <div class="confirm-info">
              <div class="confirm-row">
                <span class="confirm-label">Sản phẩm</span>
                <span class="confirm-value">{{ selectedProduct?.tenSanPham }}</span>
              </div>
              <div class="confirm-row">
                <span class="confirm-label">Mã SKU</span>
                <span class="confirm-value mono">{{ selectedProduct?.maSku }}</span>
              </div>
              <div class="confirm-row">
                <span class="confirm-label">Số Serial</span>
                <span class="confirm-value mono">{{ selectedProduct?.soSerial }}</span>
              </div>
              <div class="confirm-row">
                <span class="confirm-label">Ngày mua</span>
                <span class="confirm-value">{{ selectedProduct?.ngayMua }}</span>
              </div>
              <div class="confirm-row">
                <span class="confirm-label">Hết hạn BH hiện tại</span>
                <span class="confirm-value">{{ selectedProduct?.ngayHetBh }}</span>
              </div>
            </div>

            <div class="confirm-plan-highlight" :class="`confirm-plan-highlight--${selectedPlan?.color}`">
              <div class="highlight-label">
                <Shield :size="14" />
                Gói đăng ký
              </div>
              <div class="highlight-name">{{ selectedPlan?.name }}</div>
              <div class="highlight-detail">
                Gia hạn <strong>{{ selectedPlan?.durationLabel }}</strong>
                từ <strong>{{ selectedProduct?.ngayHetBh }}</strong>
                đến <strong>{{ new Date(new Date(selectedProduct?.ngayHetBh).getTime() + (selectedPlan?.duration || 0) * 30 * 86400000).toLocaleDateString('vi-VN') }}</strong>
              </div>
              <div class="highlight-price">{{ selectedPlan ? formatPrice(selectedPlan.price) : '—' }}</div>
            </div>

            <div class="confirm-warning">
              <Info :size="13" />
              Sau khi đăng ký, yêu cầu sẽ được nhân viên xác nhận trong vòng 24h làm việc.
              Bạn sẽ nhận thông báo qua email và tài khoản này.
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn-ghost" @click="closeRegister" :disabled="sending">Hủy</button>
            <button class="btn-primary" @click="submitRegistration" :disabled="sending">
              <RefreshCw v-if="sending" :size="14" class="spin" />
              <ShieldPlus v-else :size="14" />
              {{ sending ? 'Đang gửi yêu cầu...' : 'Xác nhận đăng ký' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.warranty-plan-tab { display: flex; flex-direction: column; gap: 18px; }

/* Hero */
.plan-hero {
  background: linear-gradient(135deg, var(--pink-500) 0%, var(--pink-600) 100%);
  border-radius: 14px;
  padding: 18px 20px;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.plan-hero-content { display: flex; gap: 14px; align-items: center; flex: 1; min-width: 0; }
.plan-hero-icon {
  width: 56px; height: 56px;
  background: rgba(255,255,255,0.18);
  border: 1px solid rgba(255,255,255,0.25);
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.plan-hero-title { font-weight: 800; font-size: 1.1rem; line-height: 1.3; }
.plan-hero-desc { font-size: 12.5px; opacity: 0.85; margin-top: 4px; line-height: 1.5; }
.plan-hero-stats { display: flex; gap: 10px; }
.hero-stat {
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 10px;
  padding: 8px 14px;
  text-align: center;
  min-width: 80px;
}
.hero-stat-value { font-weight: 800; font-size: 1rem; }
.hero-stat-label { font-size: 10.5px; opacity: 0.85; margin-top: 2px; display: inline-flex; align-items: center; gap: 3px; }

/* Plan Grid */
.plan-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 12px;
}
.plan-card {
  position: relative;
  background: var(--bg-card);
  border: 2px solid var(--border);
  border-radius: 14px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: all 0.2s ease;
}
.plan-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--sh-2);
}
.plan-card--popular {
  border-color: var(--gold, #f59e0b);
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.15);
}
.plan-card--blue:hover { border-color: var(--info, #2563eb); }
.plan-card--gold:hover { border-color: var(--gold, #f59e0b); }
.plan-card--platinum { background: linear-gradient(135deg, #1f2937 0%, #374151 100%); color: white; border-color: #4b5563; }
.plan-card--platinum:hover { border-color: var(--pink-400); }

.plan-popular-badge {
  position: absolute;
  top: -10px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--gold, #f59e0b);
  color: white;
  padding: 4px 12px;
  border-radius: 9999px;
  font-size: 10.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.plan-discount-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: var(--pink-100);
  color: var(--pink-600);
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 700;
}

.plan-card-header {
  display: flex;
  gap: 10px;
  align-items: center;
}
.plan-icon {
  width: 44px; height: 44px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.plan-icon--blue { background: rgba(37, 99, 235, 0.12); color: var(--info, #2563eb); }
.plan-icon--gold { background: rgba(245, 158, 11, 0.15); color: var(--gold, #f59e0b); }
.plan-icon--platinum { background: rgba(255,255,255,0.15); color: #fbbf24; }

.plan-name { font-weight: 700; font-size: 13.5px; line-height: 1.3; color: var(--text-primary); }
.plan-card--platinum .plan-name { color: white; }
.plan-duration {
  font-size: 11.5px;
  color: var(--text-secondary);
  margin-top: 3px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.plan-card--platinum .plan-duration { color: rgba(255,255,255,0.7); }
.plan-duration strong { color: var(--text-primary); }
.plan-card--platinum .plan-duration strong { color: white; }

.plan-price-block { display: flex; flex-direction: column; gap: 2px; }
.plan-price-row { display: flex; align-items: baseline; gap: 8px; }
.plan-price-current {
  font-size: 1.3rem;
  font-weight: 800;
  color: var(--pink-600);
}
.plan-card--platinum .plan-price-current { color: #fbbf24; }
.plan-price-original {
  font-size: 12px;
  color: var(--text-muted);
  text-decoration: line-through;
}
.plan-discount-text {
  font-size: 10.5px;
  color: var(--pink-600);
  font-weight: 700;
}
.plan-card--platinum .plan-discount-text { color: #fbbf24; }

.plan-features {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.plan-feature {
  display: flex;
  gap: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-secondary);
}
.plan-card--platinum .plan-feature { color: rgba(255,255,255,0.85); }
.plan-feature :first-child {
  flex-shrink: 0;
  margin-top: 2px;
  color: var(--success, #16a34a);
}
.plan-card--platinum .plan-feature :first-child { color: #fbbf24; }

.plan-cta {
  margin-top: auto;
  padding: 9px 14px;
  background: var(--pink-500);
  color: white;
  border: none;
  border-radius: 9999px;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 3px 0 var(--pink-700);
}
.plan-cta:hover {
  background: var(--pink-600);
  transform: translateY(-1px);
}
.plan-cta:active {
  transform: translateY(1px);
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.15);
}
.plan-card--platinum .plan-cta {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  box-shadow: 0 3px 0 #b45309;
}
.plan-card--platinum .plan-cta:hover { filter: brightness(1.1); }

/* Eligible */
.eligible-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
}
.eligible-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 12px;
}
.eligible-title :first-child { color: var(--pink-500); }

.count-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: var(--pink-100);
  color: var(--pink-600);
  border-radius: 9999px;
  font-size: 10.5px;
  font-weight: 700;
}

.order-group { display: flex; flex-direction: column; gap: 10px; }
.order-block {
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}
.order-summary {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-card);
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}
.order-summary:hover { background: var(--pink-50); }
.order-info { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; font-size: 12.5px; }
.order-label { color: var(--text-muted); }
.order-code { color: var(--pink-600); }
.order-dot { color: var(--text-muted); }
.order-date { color: var(--text-secondary); }
.order-summary-right { display: flex; align-items: center; gap: 10px; }
.product-count {
  background: var(--pink-100);
  color: var(--pink-600);
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 10.5px;
  font-weight: 700;
}
.order-summary svg {
  transition: transform 0.2s ease;
  color: var(--text-muted);
}
.order-summary svg.is-open { transform: rotate(180deg); }

.order-products-list { padding: 0 16px 16px; display: flex; flex-direction: column; gap: 10px; }
.product-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.product-thumb {
  width: 60px; height: 60px;
  background: var(--pink-50);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--pink-400);
  flex-shrink: 0;
  overflow: hidden;
}
.product-thumb img { width: 100%; height: 100%; object-fit: contain; padding: 4px; }
.product-info { flex: 1; min-width: 200px; }
.product-name {
  font-weight: 700;
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.4;
}
.product-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 3px;
  flex-wrap: wrap;
}
.product-meta code {
  background: var(--gray-100);
  padding: 1px 5px;
  border-radius: 4px;
  font-family: 'SF Mono', monospace;
  font-size: 10.5px;
}
.product-warranty {
  font-size: 11.5px;
  color: var(--text-secondary);
  margin-top: 6px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.product-warranty strong { color: var(--pink-600); }

.product-plans {
  width: 100%;
  border-top: 1px dashed var(--border);
  padding-top: 10px;
  margin-top: 4px;
}
.product-plans-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 6px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.plan-buttons {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 6px;
}
.plan-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 7px 10px;
  background: var(--bg-card);
  border: 1.5px solid var(--border);
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}
.plan-btn:hover { transform: translateY(-1px); border-color: var(--pink-500); }
.plan-btn--blue { background: rgba(37, 99, 235, 0.04); }
.plan-btn--gold { background: rgba(245, 158, 11, 0.05); }
.plan-btn--platinum { background: rgba(31, 41, 55, 0.06); }
.plan-btn strong {
  margin-left: auto;
  color: var(--pink-600);
  font-weight: 800;
  font-size: 12.5px;
}

/* Loading & Empty */
.loading-state { display: flex; flex-direction: column; gap: 8px; }
.empty-state-card {
  text-align: center;
  padding: 30px 16px;
  background: var(--gray-50);
  border-radius: 10px;
}
.empty-icon { color: var(--text-muted); margin-bottom: 8px; }
.empty-title { font-weight: 700; font-size: 13px; color: var(--text-primary); }
.empty-desc { font-size: 12px; color: var(--text-secondary); margin-top: 4px; }

/* Slide animation */
.slide-down-enter-active, .slide-down-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
  max-height: 1200px;
}
.slide-down-enter-from, .slide-down-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(31, 41, 55, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
  padding: 20px;
  backdrop-filter: blur(4px);
}
.modal-content {
  background: var(--bg-card);
  border-radius: 16px;
  width: 100%;
  max-width: 540px;
  max-height: 92vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0,0,0,0.25);
}
.modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(180deg, var(--pink-50) 0%, var(--bg-card) 100%);
}
.modal-title { display: flex; gap: 10px; align-items: center; color: var(--pink-500); }
.modal-title-main { font-weight: 800; font-size: 1rem; color: var(--text-primary); }
.modal-title-sub {
  font-size: 12.5px;
  color: var(--text-secondary);
  margin-top: 2px;
  font-weight: 500;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.modal-close {
  width: 32px; height: 32px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-close:hover { background: var(--gray-100); color: var(--text-primary); }

.modal-body { padding: 18px 20px; overflow-y: auto; }
.confirm-info {
  background: var(--gray-50);
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.confirm-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 12.5px;
}
.confirm-label { color: var(--text-muted); }
.confirm-value { color: var(--text-primary); font-weight: 600; text-align: right; }
.mono { font-family: 'SF Mono', monospace; font-size: 11.5px; }

.confirm-plan-highlight {
  border-radius: 12px;
  padding: 14px;
  margin-top: 14px;
  text-align: center;
}
.confirm-plan-highlight--blue {
  background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
  border: 2px solid rgba(37, 99, 235, 0.2);
}
.confirm-plan-highlight--gold {
  background: linear-gradient(135deg, #fef3c7 0%, #fffbeb 100%);
  border: 2px solid rgba(245, 158, 11, 0.2);
}
.confirm-plan-highlight--platinum {
  background: linear-gradient(135deg, #1f2937 0%, #374151 100%);
  color: white;
}
.highlight-label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
}
.confirm-plan-highlight--platinum .highlight-label { color: rgba(255,255,255,0.7); }
.highlight-name { font-weight: 800; font-size: 14px; color: var(--text-primary); }
.confirm-plan-highlight--platinum .highlight-name { color: white; }
.highlight-detail {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 8px;
  line-height: 1.5;
}
.confirm-plan-highlight--platinum .highlight-detail { color: rgba(255,255,255,0.85); }
.highlight-detail strong { color: var(--pink-600); }
.confirm-plan-highlight--platinum .highlight-detail strong { color: #fbbf24; }
.highlight-price {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--pink-600);
  margin-top: 8px;
}
.confirm-plan-highlight--platinum .highlight-price { color: #fbbf24; }

.confirm-warning {
  margin-top: 14px;
  background: var(--gray-50);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 11.5px;
  color: var(--text-secondary);
  display: flex;
  gap: 8px;
  align-items: flex-start;
  line-height: 1.5;
}
.confirm-warning :first-child { flex-shrink: 0; color: var(--info, #2563eb); margin-top: 2px; }

.modal-footer {
  padding: 14px 20px;
  border-top: 1px solid var(--border);
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  background: var(--gray-50);
}
.btn-primary, .btn-ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 9px 18px;
  border: none;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-primary {
  background: var(--pink-500);
  color: white;
  box-shadow: 0 3px 0 var(--pink-700);
}
.btn-primary:hover:not(:disabled) { background: var(--pink-600); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; box-shadow: none; }
.btn-ghost {
  background: var(--bg-card);
  color: var(--text-secondary);
  border: 1px solid var(--border);
}
.btn-ghost:hover:not(:disabled) { background: var(--gray-100); }
.btn-ghost:disabled { opacity: 0.5; cursor: not-allowed; }

.spin { animation: spin 1s linear infinite; }
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }

@media (max-width: 575.98px) {
  .plan-hero { flex-direction: column; align-items: stretch; }
  .plan-hero-stats { justify-content: center; }
  .product-card { flex-direction: column; }
  .product-thumb { width: 100%; height: 100px; }
}
</style>
