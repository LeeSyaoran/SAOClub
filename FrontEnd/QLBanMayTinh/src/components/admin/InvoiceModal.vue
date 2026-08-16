<script setup>
import { ref, computed, watch, onMounted } from "vue";
import { formatPrice, formatDateTime } from "../../utils/adminFormat.js";
import { paymentMethodLabel } from "../../utils/orderStatus.js";
import * as ChiTietDonHangService from "../../services/ChiTietDonHangService.js";
import * as ThanhToanService from "../../services/ThanhToanService.js";
import * as KhachHangService from "../../services/KhachHangService.js";
import { ProductsStore, ensureProducts } from "../../stores/products.js";
import { X, Printer } from "@lucide/vue";

onMounted(() => ensureProducts());

// ─── Props ───────────────────────────────────────────────────────────────────
const props = defineProps({
  show:      { type: Boolean, default: false },
  donHangId: { type: [Number, String], default: null },
  order:     { type: Object, default: null },        // DonHangResponse (đã có sẵn)
  customer:  { type: Object, default: null },        // KhachHangResponse nếu đã biết
});
const emit = defineEmits(["close"]);

// ─── State ───────────────────────────────────────────────────────────────────
const loading   = ref(false);
const items     = ref([]);   // ChiTietDonHangResponse[]
const payments  = ref([]);   // ThanhToanResponse[]
const custData  = ref(null); // KhachHangResponse

// 1 điểm = 10.000đ mua hàng
const DIEM_RATE = 10_000;

const diemCong = computed(() => {
  if (!props.order) return 0;
  return Math.floor((props.order.thanhTien ?? props.order.tongTien ?? 0) / DIEM_RATE);
});

const tongDiem = computed(() => {
  const kh = custData.value ?? props.customer;
  if (!kh) return null;
  return (kh.diemTichLuy ?? 0) + diemCong.value;
});

// ─── Load data khi mở modal ──────────────────────────────────────────────────
watch(() => props.show, async (open) => {
  if (!open || !props.donHangId) return;
  loading.value = true;
  try {
    const [its, pays] = await Promise.all([
      ChiTietDonHangService.getByDonHang(props.donHangId).catch(() => []),
      ThanhToanService.getByDonHang(props.donHangId).catch(() => []),
    ]);
    items.value    = its;
    payments.value = pays;

    // Load khách nếu chưa có
    if (!props.customer && props.order?.khachHangId) {
      custData.value = await KhachHangService.getById(props.order.khachHangId).catch(() => null);
    } else {
      custData.value = props.customer ?? null;
    }
  } finally {
    loading.value = false;
  }
}, { immediate: true });

// ─── Helpers ─────────────────────────────────────────────────────────────────
const kh = computed(() => custData.value ?? props.customer);
const grandTotal = computed(() => props.order?.thanhTien ?? props.order?.tongTien ?? 0);

// Tra cuu san pham theo bienTheId tu ProductsStore (da flatten san pham + bien the)
const productByBienThe = (bienTheId) =>
  (ProductsStore.items ?? []).find(p => p.bienTheId === bienTheId);

// Ten san pham: uu tien item.tenSanPham, fallback lookup store, fallback tenBienThe
const productName = (item) =>
  item.tenSanPham ??
  productByBienThe(item.bienTheId)?.tenSanPham ??
  item.tenBienThe ?? '—';

const variantLine = (item) => {
  // Thu lay tu store neu item khong co
  const sp = productByBienThe(item.bienTheId);
  const parts = [];
  const cpu   = item.cpu   ?? sp?.cpu;
  const ram   = item.ram   ?? sp?.ram;
  const oCung = item.oCung ?? sp?.oCung;
  const mau   = item.mauSac ?? sp?.mauSac;
  const sku   = item.maSku  ?? sp?.maSku;
  if (cpu)   parts.push(cpu);
  if (ram)   parts.push(ram);
  if (oCung) parts.push(oCung);
  if (mau)   parts.push(mau);
  return parts.join(' / ');
};

const skuLine = (item) => {
  const sp = productByBienThe(item.bienTheId);
  return item.maSku ?? sp?.maSku ?? null;
};

// So thanh chu tieng Viet
const VN_ONES = ["","một","hai","ba","bốn","năm","sáu","bảy","tám","chín"];
const VN_TENS = ["","mười","hai mươi","ba mươi","bốn mươi","năm mươi","sáu mươi","bảy mươi","tám mươi","chín mươi"];
function doc3(n) {
  if (!n) return "";
  const h = Math.floor(n/100), t = Math.floor((n%100)/10), u = n%10;
  let s = h ? VN_ONES[h]+" trăm" : "";
  if (t) s += (s?" ":"")+VN_TENS[t]; else if (h&&u) s += " linh";
  if (u) s += (s?" ":"")+((t===1&&u===5)?"lăm":VN_ONES[u]);
  return s;
}
function soThanhChu(n) {
  if (!n) return "Không đồng";
  const ty=Math.floor(n/1e9),tr=Math.floor((n%1e9)/1e6),ng=Math.floor((n%1e6)/1e3),du=n%1e3;
  const p=[];
  if(ty) p.push(doc3(ty)+" tỷ"); if(tr) p.push(doc3(tr)+" triệu");
  if(ng) p.push(doc3(ng)+" nghìn"); if(du) p.push(doc3(du));
  const s=p.join(" ")+" đồng chẵn"; return s.charAt(0).toUpperCase()+s.slice(1);
}
const totalInWords = computed(() => soThanhChu(grandTotal.value));
const esc = (v) => String(v??"").replace(/[&<>]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;"}[c]));

const kenhBanLabel = (k) => k === 'in_store' ? 'Tại quầy' : 'Online';

// ─── Print (Vietnamese retail receipt style) ─────────────────────────────────
const printInvoice = () => {
  const o = props.order, c = kh.value, now = new Date().toLocaleString("vi-VN");
  const rows = items.value.map((item,i) => {
    const vl = variantLine(item), sn = item.soSerial ?? item.serialNumber ?? "";
    const pname = productName(item);
    return `<tr>
      <td style="text-align:center;border:1px solid #ccc;">${i+1}</td>
      <td style="border:1px solid #ccc;padding:5px 7px;">
        <strong>${esc(pname)}</strong>
        ${vl?`<br><span style="font-size:10px;color:#555;">${esc(vl)}</span>`:""}
        ${skuLine(item)?`<br><span style="font-size:10px;color:#555;">Mã hàng: <strong>${esc(skuLine(item))}</strong></span>`:""}
        <br><span style="font-size:10px;color:#555;">Số serial: <strong>${esc(sn||"—")}</strong></span>
      </td>
      <td style="text-align:center;border:1px solid #ccc;">Cái</td>
      <td style="text-align:center;border:1px solid #ccc;">1</td>
      <td style="text-align:right;border:1px solid #ccc;">${esc(formatPrice(item.giaBan??item.donGia))}</td>
      <td style="text-align:right;font-weight:700;border:1px solid #ccc;">${esc(formatPrice(item.giaBan??item.donGia))}</td>
    </tr>`;
  }).join("");
  const payText = payments.value.map(p =>
    esc(paymentMethodLabel(p.phuongThucThanhToan))+": "+esc(formatPrice(p.soTien))
  ).join(" · ");
  const docTitle = `HoaDon_${(o?.maDonHang ?? `DH${o?.donHangId}`).replace(/[^a-zA-Z0-9]/g,'')}_${(c?.hoTen ?? o?.nguoiNhan ?? 'KhachLe').replace(/\s+/g,'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-zA-Z0-9]/g,'')}`;
  const html = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><title>${esc(docTitle)}</title>
<style>*{box-sizing:border-box;margin:0;padding:0;-webkit-print-color-adjust:exact;print-color-adjust:exact;}body{font-family:"Times New Roman",serif;font-size:12px;color:#111;padding:20px 26px;max-width:700px;margin:0 auto;}
.hb{background:#111;color:#fff;text-align:center;padding:10px 14px;border-radius:2px;margin-bottom:12px;}
.hb h2{font-size:15px;font-weight:900;}.hb p{font-size:11px;color:#ccc;margin-top:2px;}
.st{text-align:center;margin-bottom:12px;padding-bottom:10px;border-bottom:2px solid #111;}
.sn{font-size:19px;font-weight:900;}.si{font-size:11px;color:#555;margin-top:1px;}
.sec{font-weight:700;font-size:11px;text-align:center;text-transform:uppercase;letter-spacing:.07em;border-top:1px solid #999;border-bottom:1px solid #999;padding:3px 0;margin:8px 0;}
.ct{width:100%;font-size:11.5px;border-collapse:collapse;margin-bottom:4px;}
.ct td{padding:2px 0;vertical-align:top;}.ct td:first-child{width:110px;font-weight:600;color:#444;}
table.p{width:100%;border-collapse:collapse;font-size:12px;}
table.p th{background:#111;color:#fff;padding:5px 6px;font-size:11px;border:1px solid #111;text-align:center;}
.sub td{background:#f5f5f5;font-weight:700;padding:4px 6px;border:1px solid #ccc;}
.dis td{color:green;padding:3px 6px;border:1px solid #ccc;}
.gr td{background:#111;color:#fff;font-weight:900;font-size:14px;padding:7px 6px;border:1px solid #111;}
.w{font-size:11.5px;font-style:italic;border:1px dashed #aaa;padding:6px 10px;margin:8px 0;}
.pts{background:#fffbea;border:1px solid #c8a400;border-radius:3px;padding:8px 12px;margin:8px 0;}
.ph{font-weight:800;color:#7a5c00;font-size:12px;margin-bottom:4px;}
.pr{display:flex;justify-content:space-between;font-size:12px;}.pb{font-size:20px;font-weight:900;color:#b8860b;}
.foot{font-size:10.5px;color:#555;text-align:center;border-top:1px solid #ddd;padding-top:8px;margin-top:10px;line-height:1.8;}
.bar{text-align:center;margin-top:8px;font-family:monospace;font-size:10px;letter-spacing:.14em;color:#333;}
.meta{display:flex;justify-content:space-between;font-size:10px;color:#666;margin-top:6px;}
@media print{body{padding:4px 8px;}}</style></head><body>
<div class="hb"><h2>HÓA ĐƠN BÁN HÀNG</h2><p>Số: ${esc(o?.maDonHang??`#${o?.donHangId}`)}</p></div>
<div class="st"><div class="sn">⭐ SAO CLUB</div><div class="si">Cửa hàng máy tính & công nghệ · Hotline: 1900-xxxx</div></div>
<div class="sec">Thông tin khách hàng</div>
<table class="ct">
<tr><td>Khách hàng:</td><td><strong>${esc(c?.hoTen??o?.nguoiNhan??"Khách lẻ")}</strong></td></tr>
<tr><td>Điện thoại:</td><td>${esc(c?.soDienThoai??o?.sdtNguoiNhan??"—")}</td></tr>
${c?.email?`<tr><td>Email:</td><td>${esc(c.email)}</td></tr>`:""}
${(o?.diaChiGiaoHangText??c?.diaChi)?`<tr><td>Địa chỉ:</td><td>${esc(o?.diaChiGiaoHangText??c?.diaChi)}</td></tr>`:""}
<tr><td>Hình thức:</td><td>${o?.kenhBan==="in_store"?"Mua tại quầy":"Mua hàng online"}</td></tr>
</table>
<div class="sec">Danh sách sản phẩm</div>
<table class="p"><thead><tr><th style="width:28px;">STT</th><th>Tên hàng hóa</th><th style="width:34px;">ĐVT</th><th style="width:26px;">SL</th><th style="width:100px;">Đơn giá</th><th style="width:100px;">Thành tiền</th></tr></thead>
<tbody>${rows}</tbody>
<tfoot>
<tr class="sub"><td colspan="5" style="text-align:right;">Cộng tiền hàng:</td><td style="text-align:right;">${esc(formatPrice(o?.tongTien??0))}</td></tr>
${(o?.giamGia??0)>0?`<tr class="dis"><td colspan="5" style="text-align:right;">Giảm giá:</td><td style="text-align:right;">- ${esc(formatPrice(o.giamGia))}</td></tr>`:""}
${(o?.phiVanChuyen??0)>0?`<tr><td colspan="5" style="text-align:right;border:1px solid #ccc;">Phí vận chuyển:</td><td style="text-align:right;border:1px solid #ccc;">${esc(formatPrice(o.phiVanChuyen))}</td></tr>`:""}
<tr class="gr"><td colspan="5" style="text-align:right;">Tổng cộng tiền thanh toán:</td><td style="text-align:right;">${esc(formatPrice(grandTotal.value))} VNĐ</td></tr>
</tfoot></table>
<div class="w"><strong>Số tiền viết bằng chữ:</strong> ${esc(totalInWords.value)}</div>
${payments.value.length?`<p style="font-size:12px;margin:4px 0;"><strong>Thanh toán:</strong> ${payText}</p>`:""}
<div class="pts"><div class="ph">★ ĐIỂM THƯỞNG TÍCH LŨY</div>
<div class="pr"><span>Điểm cộng từ đơn này:</span><span class="pb">+${diemCong.value} điểm</span></div>
${tongDiem.value!==null?`<div class="pr" style="color:#555;margin-top:2px;font-weight:600;"><span>Tổng điểm tích lũy:</span><span>${tongDiem.value} điểm</span></div>`:""}
<div style="font-size:10.5px;color:#888;margin-top:3px;">1 điểm = ${esc(formatPrice(DIEM_RATE))} · Dùng để quay vòng quay may mắn</div></div>
<div class="meta"><span>HĐ: ${esc(o?.maDonHang??`#${o?.donHangId}`)}</span><span>${esc(formatDateTime(o?.ngayDat))}</span><span>In: ${now}</span></div>
<div class="bar">|||||||||||||||||||||||||||||||||||||||||||||||||||<br>${esc(String(o?.donHangId??"").padStart(13,"0"))}</div>
<div class="foot">Bảo hành: <strong>24 tháng</strong> Laptop · <strong>12 tháng</strong> Phụ kiện<br>Hàng đã mua không đổi/trả trừ lỗi nhà sản xuất<br><strong>Trân trọng cảm ơn quý khách! 🙏</strong></div>
</body></html>`;
  const iframe = document.createElement("iframe");
  iframe.style.cssText = "position:fixed;width:0;height:0;border:0;visibility:hidden;";
  document.body.appendChild(iframe);
  iframe.contentDocument.write(html);
  iframe.contentDocument.close();
  iframe.onload = () => { iframe.contentWindow.print(); setTimeout(()=>document.body.removeChild(iframe),600); };
};
</script>

<template>
  <Teleport to="body">
    <div v-if="show" class="inv-backdrop" @click.self="emit('close')">
      <div class="inv-shell">

        <!-- Top bar -->
        <div class="inv-topbar">
          <div>
            <div class="inv-topbar-title">Hóa đơn bán hàng</div>
            <div class="inv-topbar-sub">{{ order?.maDonHang ?? `#${order?.donHangId}` }} · {{ formatDateTime(order?.ngayDat) }}</div>
          </div>
          <div style="display:flex;gap:8px;align-items:center;">
            <button class="inv-close-btn" @click="emit('close')"><X :size="16" /></button>
          </div>
        </div>

        <!-- Scroll / paper -->
        <div class="inv-scroll">
          <div v-if="loading" class="inv-loading">Đang tải...</div>
          <div v-else class="inv-paper">

            <div class="inv-hdr-blk">
              <div class="inv-hdr-main">HÓA ĐƠN BÁN HÀNG</div>
              <div class="inv-hdr-sub">Số: {{ order?.maDonHang ?? `#${order?.donHangId}` }}</div>
            </div>

            <div class="inv-store">
              <div class="inv-store-name">⭐ SAO CLUB</div>
              <div class="inv-store-info">Cửa hàng máy tính & công nghệ · Hotline: 1900-xxxx</div>
            </div>

            <hr class="inv-hr" />
            <div class="inv-sec-title">THÔNG TIN KHÁCH HÀNG</div>
            <table class="inv-cust">
              <tr><td>Khách hàng:</td><td><strong>{{ kh?.hoTen ?? order?.nguoiNhan ?? 'Khách lẻ' }}</strong></td></tr>
              <tr><td>Điện thoại:</td><td>{{ kh?.soDienThoai ?? order?.sdtNguoiNhan ?? '—' }}</td></tr>
              <tr v-if="kh?.email"><td>Email:</td><td>{{ kh.email }}</td></tr>
              <tr v-if="order?.diaChiGiaoHangText||kh?.diaChi"><td>Địa chỉ:</td><td>{{ order?.diaChiGiaoHangText??kh?.diaChi }}</td></tr>
              <tr>
                <td>Hình thức:</td>
                <td><span :class="order?.kenhBan==='in_store'?'inv-badge-s':'inv-badge-o'">{{ order?.kenhBan==='in_store'?'Mua tại quầy':'Mua hàng online' }}</span></td>
              </tr>
            </table>

            <hr class="inv-hr" />
            <div class="inv-sec-title">DANH SÁCH SẢN PHẨM</div>
            <table class="inv-prod">
              <thead>
                <tr>
                  <th style="width:32px;">STT</th>
                  <th>Tên hàng hóa</th>
                  <th style="width:36px;">ĐVT</th>
                  <th style="width:28px;">SL</th>
                  <th style="width:100px;">Đơn giá</th>
                  <th style="width:100px;">Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, idx) in items" :key="idx">
                  <td class="tc">{{ idx+1 }}</td>
                  <td>
                    <div class="inv-pname">{{ productName(item) }}</div>
                    <div v-if="variantLine(item)" class="inv-pvar">{{ variantLine(item) }}</div>
                    <div v-if="skuLine(item)" class="inv-pser">Mã hàng: <strong>{{ skuLine(item) }}</strong></div>
                    <div class="inv-pser">Số serial: <strong>{{ item.soSerial ?? item.serialNumber ?? '—' }}</strong></div>
                  </td>
                  <td class="tc">Cái</td>
                  <td class="tc">1</td>
                  <td class="tr">{{ formatPrice(item.giaBan ?? item.donGia) }}</td>
                  <td class="tr inv-amt">{{ formatPrice(item.giaBan ?? item.donGia) }}</td>
                </tr>
                <tr v-if="!items.length">
                  <td colspan="6" class="tc" style="color:#999;padding:12px;">Đang tải...</td>
                </tr>
              </tbody>
              <tfoot>
                <tr class="inv-sub"><td colspan="5">Cộng tiền hàng:</td><td class="tr">{{ formatPrice(order?.tongTien??0) }}</td></tr>
                <tr v-if="(order?.giamGia??0)>0" class="inv-dis"><td colspan="5">Giảm giá:</td><td class="tr">- {{ formatPrice(order.giamGia) }}</td></tr>
                <tr v-if="(order?.phiVanChuyen??0)>0" class="inv-fee"><td colspan="5">Phí vận chuyển:</td><td class="tr">{{ formatPrice(order.phiVanChuyen) }}</td></tr>
                <tr class="inv-grand"><td colspan="5">Tổng cộng tiền thanh toán:</td><td class="tr">{{ formatPrice(grandTotal) }} VNĐ</td></tr>
              </tfoot>
            </table>

            <div class="inv-words"><span class="inv-wlbl">Số tiền viết bằng chữ: </span>{{ totalInWords }}</div>

            <div v-if="payments.length" class="inv-pay">
              <strong>Thanh toán:</strong>
              {{ payments.map(p => paymentMethodLabel(p.phuongThucThanhToan)).join(' · ') }}
            </div>

            <hr class="inv-hr" />

            <div class="inv-points">
              <div class="inv-pts-hdr">★ ĐIỂM THƯỞNG TÍCH LŨY</div>
              <div class="inv-pts-row">
                <span>Điểm cộng từ đơn này:</span>
                <span class="inv-pts-big">+{{ diemCong }} điểm</span>
              </div>
              <div v-if="tongDiem !== null" class="inv-pts-total">
                <span>Tổng điểm tích lũy:</span><span>{{ tongDiem }} điểm</span>
              </div>
              <div class="inv-pts-note">1 điểm = {{ formatPrice(DIEM_RATE) }} · Dùng để quay vòng quay may mắn</div>
            </div>

            <hr class="inv-hr" />

            <div class="inv-meta">
              <span>HĐ: {{ order?.maDonHang ?? `#${order?.donHangId}` }}</span>
              <span>{{ formatDateTime(order?.ngayDat) }}</span>
            </div>

            <div class="inv-barcode">
              <div class="inv-bars">
                <span v-for="i in 58" :key="i" :style="{width: i%3===0?'3px':i%5===0?'1px':'2px'}"></span>
              </div>
              <div class="inv-barnum">{{ String(order?.donHangId??'').padStart(13,'0') }}</div>
            </div>

            <div class="inv-foot">
              Bảo hành: <strong>24 tháng</strong> Laptop · <strong>12 tháng</strong> Phụ kiện<br>
              Hàng đã mua không đổi/trả trừ lỗi nhà sản xuất<br>
              <strong>Trân trọng cảm ơn quý khách! 🙏</strong>
            </div>

          </div>
        </div>

        <!-- Bottom bar -->
        <div class="inv-footer">
          <button class="inv-btn-close" @click="emit('close')">Đóng</button>
          <button class="inv-btn-print" @click="printInvoice"><Printer :size="14" style="margin-right:4px;" /> In hóa đơn</button>
        </div>

      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.inv-backdrop{position:fixed;inset:0;background:rgba(0,0,0,.78);z-index:2000;display:flex;align-items:center;justify-content:center;padding:16px;}
.inv-shell{width:680px;max-width:96vw;max-height:93vh;background:#18181b;border:1px solid #333;border-radius:12px;display:flex;flex-direction:column;overflow:hidden;box-shadow:0 24px 64px rgba(0,0,0,.8);}
.inv-topbar{display:flex;justify-content:space-between;align-items:center;padding:14px 20px;background:#111;border-bottom:1px solid #333;flex-shrink:0;}
.inv-topbar-title{font-weight:700;color:#fff;font-size:.95rem;}
.inv-topbar-sub{font-size:.72rem;color:#888;margin-top:1px;}
.inv-close-btn{background:none;border:1px solid #444;color:#aaa;border-radius:6px;width:30px;height:30px;display:flex;align-items:center;justify-content:center;cursor:pointer;}
.inv-close-btn:hover{background:#333;color:#fff;}
.inv-scroll{overflow-y:auto;flex:1;padding:20px;background:#111;}
.inv-loading{text-align:center;color:#888;padding:40px;}
.inv-paper{background:#fff;border-radius:3px;padding:24px 28px;color:#111;font-family:"Times New Roman",Times,serif;font-size:13px;max-width:540px;margin:0 auto;box-shadow:0 4px 24px rgba(0,0,0,.5);}
.inv-hdr-blk{background:#111;color:#fff;text-align:center;padding:10px 16px;border-radius:2px;margin-bottom:14px;}
.inv-hdr-main{font-size:15px;font-weight:900;letter-spacing:.05em;}
.inv-hdr-sub{font-size:11px;color:#ccc;margin-top:2px;}
.inv-store{text-align:center;margin-bottom:12px;}
.inv-store-name{font-size:20px;font-weight:900;letter-spacing:.04em;}
.inv-store-info{font-size:11px;color:#555;margin-top:2px;}
.inv-hr{border:none;border-top:1px solid #bbb;margin:10px 0;}
.inv-sec-title{font-weight:700;font-size:11px;text-align:center;text-transform:uppercase;letter-spacing:.08em;border-top:1px solid #999;border-bottom:1px solid #999;padding:3px 0;margin:8px 0;}
.inv-cust{width:100%;border-collapse:collapse;font-size:12px;margin-bottom:4px;}
.inv-cust td{padding:2px 0;vertical-align:top;}
.inv-cust td:first-child{width:110px;font-weight:600;color:#444;}
.inv-badge-s{background:#e0f5e0;color:#1a6b1a;padding:1px 8px;border-radius:20px;font-size:11px;font-weight:600;}
.inv-badge-o{background:#e0eeff;color:#1a4a8b;padding:1px 8px;border-radius:20px;font-size:11px;font-weight:600;}
.inv-prod{width:100%;border-collapse:collapse;font-size:12px;}
.inv-prod th{background:#111;color:#fff;padding:5px 6px;font-size:11px;border:1px solid #111;text-align:center;}
.inv-prod td{border:1px solid #ccc;padding:5px 6px;vertical-align:top;}
.inv-pname{font-weight:700;font-size:12px;}
.inv-pvar{font-size:10.5px;color:#555;margin-top:1px;}
.inv-pser{font-size:11px;color:#333;margin-top:2px;}
.inv-amt{font-weight:700;}
.inv-sub td{background:#f5f5f5;font-weight:700;padding:4px 6px;border:1px solid #ccc;}
.inv-dis td{color:#1a6b1a;padding:3px 6px;border:1px solid #ccc;}
.inv-fee td{padding:3px 6px;border:1px solid #ccc;}
.inv-grand td{background:#111;color:#fff;font-weight:900;font-size:14px;padding:7px 6px;border:1px solid #111;}
.inv-words{font-size:11.5px;font-style:italic;border:1px dashed #aaa;padding:6px 10px;border-radius:2px;margin:8px 0;line-height:1.5;}
.inv-wlbl{font-weight:700;font-style:normal;}
.inv-pay{font-size:12px;margin:5px 0;color:#333;}
.inv-points{background:#fffbea;border:1px solid #c8a400;border-radius:4px;padding:10px 14px;margin:6px 0;}
.inv-pts-hdr{font-weight:800;font-size:12px;color:#7a5c00;margin-bottom:5px;}
.inv-pts-row{display:flex;justify-content:space-between;align-items:center;font-size:12px;}
.inv-pts-big{font-size:22px;font-weight:900;color:#b8860b;}
.inv-pts-total{display:flex;justify-content:space-between;font-size:12px;color:#555;margin-top:3px;font-weight:600;}
.inv-pts-note{font-size:10.5px;color:#888;margin-top:4px;}
.inv-meta{display:flex;justify-content:space-between;font-size:10.5px;color:#666;margin:4px 0;}
.inv-barcode{text-align:center;margin:8px 0 4px;}
.inv-bars{display:inline-flex;align-items:stretch;height:42px;gap:1px;}
.inv-bars span{background:#111;display:block;}
.inv-barnum{font-family:monospace;font-size:10px;letter-spacing:.15em;color:#444;margin-top:2px;}
.inv-foot{font-size:11px;color:#555;text-align:center;line-height:1.8;border-top:1px solid #ddd;padding-top:8px;margin-top:6px;}
.inv-footer{padding:12px 20px;background:#111;border-top:1px solid #333;display:flex;justify-content:flex-end;gap:10px;flex-shrink:0;}
.inv-btn-close{background:transparent;border:1px solid #555;color:#aaa;padding:7px 18px;border-radius:8px;font-size:.85rem;cursor:pointer;}
.inv-btn-close:hover{background:#222;color:#fff;}
.inv-btn-print{background:#f5c518;border:none;color:#111;padding:7px 18px;border-radius:8px;font-size:.85rem;font-weight:700;cursor:pointer;display:flex;align-items:center;}
.inv-btn-print:hover{background:#e0b000;}
.tc{text-align:center;}
.tr{text-align:right;}
</style>
