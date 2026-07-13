<template>
  <div class="position-relative">
    <input v-model="query" @input="onInput" @focus="onFocus" @blur="onBlur"
           class="form-control form-control-sm"
           style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);border-radius:10px;"
           :placeholder="placeholder" />

    <!-- Gợi ý địa chỉ (Nominatim/OpenStreetMap) -->
    <div v-if="showSuggestions && suggestions.length" class="position-absolute w-100 rounded-3 mt-1 shadow-lg"
         style="z-index:20; background:var(--bg-card); border:1px solid var(--border-color-strong); max-height:220px; overflow-y:auto;">
      <div v-for="s in suggestions" :key="s.place_id" class="px-3 py-2 small"
           style="cursor:pointer;" @mousedown.prevent="selectSuggestion(s)"
           @mouseenter="$event.currentTarget.style.background='var(--bg-hover)'"
           @mouseleave="$event.currentTarget.style.background=''">
        {{ s.display_name }}
      </div>
    </div>
    <div v-if="searching" class="small mt-1" style="color:var(--text-secondary);">{{ t('checkout.addressSearching') }}</div>

    <!-- Bản đồ + ghim vị trí — chỉ hiện sau khi đã chọn 1 địa chỉ có tọa độ -->
    <div v-if="hasCoords" ref="mapEl" style="height:200px;border-radius:10px;margin-top:8px;overflow:hidden;"></div>
    <div v-if="hasCoords" class="small mt-1" style="color:var(--text-secondary);">{{ t('checkout.addressDragHint') }}</div>
  </div>
</template>

<script setup>
// Tìm địa chỉ + ghim trên bản đồ, kiểu Shopee — dùng OpenStreetMap Nominatim (miễn phí,
// không cần API key) cho gợi ý địa chỉ + reverse-geocode, và Leaflet cho bản đồ hiển thị.
// Nominatim công khai giới hạn ~1 request/giây và khuyến nghị dùng nhẹ — đủ cho 1 người
// dùng gõ tìm; nếu lượng truy cập lớn hơn cần đổi sang provider trả phí hoặc tự host.
import { ref, watch, nextTick, onBeforeUnmount } from 'vue';
import { t } from '../../i18n/index.js';
import L from 'leaflet';
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

// Leaflet mặc định trỏ icon marker theo URL tương đối trong CSS — vỡ khi qua bundler
// (Vite). Trỏ lại icon qua import tĩnh để Vite tự resolve đúng đường dẫn.
L.Icon.Default.mergeOptions({ iconRetinaUrl: markerIcon2x, iconUrl: markerIcon, shadowUrl: markerShadow });

const props = defineProps({
  modelValue:  { type: String, default: '' },
  placeholder: { type: String, default: '' },
});
const emit = defineEmits(['update:modelValue']);

const query = ref(props.modelValue);
watch(() => props.modelValue, (v) => { if (v !== query.value) query.value = v; });

const suggestions = ref([]);
const showSuggestions = ref(false);
const searching = ref(false);
const hasCoords = ref(false);
const mapEl = ref(null);
let map = null;
let marker = null;
let debounceTimer = null;

const onInput = () => {
  emit('update:modelValue', query.value);
  clearTimeout(debounceTimer);
  if (query.value.trim().length < 3) { suggestions.value = []; return; }
  debounceTimer = setTimeout(fetchSuggestions, 400);
};
const onFocus = () => { if (suggestions.value.length) showSuggestions.value = true; };
const onBlur = () => { showSuggestions.value = false; };

const fetchSuggestions = async () => {
  searching.value = true;
  try {
    const url = `https://nominatim.openstreetmap.org/search?format=json&countrycodes=vn&limit=5&q=${encodeURIComponent(query.value)}`;
    const res = await fetch(url);
    suggestions.value = res.ok ? await res.json() : [];
    showSuggestions.value = suggestions.value.length > 0;
  } catch {
    suggestions.value = [];
  } finally {
    searching.value = false;
  }
};

const initMap = async (lat, lon) => {
  hasCoords.value = true;
  await nextTick();
  if (!map) {
    map = L.map(mapEl.value).setView([lat, lon], 16);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap',
    }).addTo(map);
    marker = L.marker([lat, lon], { draggable: true }).addTo(map);
    marker.on('dragend', onMarkerDragEnd);
  } else {
    map.setView([lat, lon], 16);
    marker.setLatLng([lat, lon]);
  }
};

// Kéo ghim xong -> reverse-geocode tọa độ mới thành địa chỉ, tự cập nhật lại ô nhập.
const onMarkerDragEnd = async () => {
  const { lat, lng } = marker.getLatLng();
  try {
    const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`;
    const res = await fetch(url);
    if (res.ok) {
      const data = await res.json();
      if (data.display_name) {
        query.value = data.display_name;
        emit('update:modelValue', data.display_name);
      }
    }
  } catch { /* giữ nguyên địa chỉ cũ nếu reverse-geocode lỗi */ }
};

const selectSuggestion = async (s) => {
  query.value = s.display_name;
  emit('update:modelValue', s.display_name);
  showSuggestions.value = false;
  suggestions.value = [];
  await initMap(Number(s.lat), Number(s.lon));
};

onBeforeUnmount(() => { if (map) { map.remove(); map = null; } });
</script>
