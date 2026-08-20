<template>
  <div ref="rootEl" class="tci-root">
    <input
      ref="inputEl" v-model="text" class="tci-input"
      :placeholder="placeholder"
      autocomplete="off"
      @focus="openPanel"
      @input="openPanel"
      @keydown.down.prevent="moveHighlight(1)"
      @keydown.up.prevent="moveHighlight(-1)"
      @keydown.enter.prevent="onEnter"
      @keydown.esc="open = false"
    />

    <Teleport to="body">
      <div
        v-if="open" ref="panelEl" class="tci-panel"
        :style="{ top: panelPos.top + 'px', left: panelPos.left + 'px', minWidth: panelPos.width + 'px' }"
      >
        <div
          v-for="(opt, idx) in filteredOptions" :key="opt.value" ref="optionEls"
          class="tci-option" :class="{ 'is-active': idx === highlightedIndex }"
          @mousedown.prevent="pick(opt)" @mouseenter="highlightedIndex = idx"
        >
          {{ opt.label }}
        </div>
        <div v-if="filteredOptions.length === 0" class="tci-empty">
          {{ allowCustom && text.trim() ? `Nhấn Enter để thêm “${text.trim()}”` : 'Không có kết quả' }}
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
// Ô gõ có gợi ý xổ xuống TỰ VẼ (thay cho <input list> + <datalist> của trình duyệt —
// popup datalist không style được, mỗi máy/trình duyệt hiện 1 kiểu xấu khác nhau).
// Dùng cho các trường "gõ rồi Enter để thêm thẻ" (chọn nhiều): bấm 1 gợi ý trong danh
// sách → bắn sự kiện "pick" (component cha tự thêm vào mảng qua hàm add sẵn có, y hệt
// hành vi cũ); gõ chữ mới rồi Enter vẫn hoạt động như trước qua sự kiện "enter".
import { ref, reactive, computed, nextTick, onBeforeUnmount } from "vue";

const props = defineProps({
  modelValue: { type: String, default: "" },
  options: { type: Array, required: true }, // [{ value, label }]
  placeholder: { type: String, default: "" },
  allowCustom: { type: Boolean, default: true },
});
const emit = defineEmits(["update:modelValue", "enter", "pick"]);

const text = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v),
});

const open = ref(false);
const rootEl = ref(null);
const panelEl = ref(null);
const inputEl = ref(null);
const optionEls = ref([]);
const highlightedIndex = ref(-1);
const panelPos = reactive({ top: 0, left: 0, width: 0 });

const filteredOptions = computed(() => {
  const q = text.value.trim().toLowerCase();
  if (!q) return props.options;
  return props.options.filter((o) => o.label.toLowerCase().includes(q));
});

const updatePanelPos = () => {
  const r = rootEl.value?.getBoundingClientRect();
  if (!r) return;
  panelPos.top = r.bottom + 4;
  panelPos.left = r.left;
  panelPos.width = r.width;
};

const openPanel = () => {
  open.value = true;
  highlightedIndex.value = -1;
  updatePanelPos();
};

const pick = (opt) => {
  emit("pick", opt.value);
  open.value = false;
  nextTick(() => inputEl.value?.focus());
};

const moveHighlight = (delta) => {
  if (!open.value) { openPanel(); return; }
  const len = filteredOptions.value.length;
  if (len === 0) return;
  highlightedIndex.value = (highlightedIndex.value + delta + len) % len;
  nextTick(() => optionEls.value[highlightedIndex.value]?.scrollIntoView({ block: "nearest" }));
};

const onEnter = () => {
  const opt = filteredOptions.value[highlightedIndex.value];
  if (open.value && opt) { pick(opt); return; }
  open.value = false;
  emit("enter");
};

const onDocClick = (e) => {
  if (!open.value) return;
  if (rootEl.value?.contains(e.target)) return;
  if (panelEl.value?.contains(e.target)) return;
  open.value = false;
};
document.addEventListener("mousedown", onDocClick);
onBeforeUnmount(() => document.removeEventListener("mousedown", onDocClick));
</script>

<style scoped>
.tci-root { position: relative; width: 100%; }
.tci-input {
  width: 100%; padding: 9px 11px;
  border: 1px solid var(--field, #d9b3c6); border-radius: 9px;
  font-size: 13px; color: var(--ink, #1f2937); background: #fff; font-family: inherit;
}
.tci-input:focus { outline: none; border-color: var(--pink-500, #ec4899); box-shadow: 0 0 0 3px var(--pink-100, #ffe6f0); }

.tci-panel {
  position: fixed; z-index: 2000; max-height: 240px; overflow-y: auto;
  background: #fff; border: 1px solid var(--line, #f1dbe6); border-radius: 12px;
  box-shadow: 0 12px 32px rgba(168, 27, 93, .18); padding: 6px;
}
.tci-option {
  padding: 8px 10px; border-radius: 8px; font-size: 13px; cursor: pointer;
  color: var(--ink, #1f2937);
}
.tci-option.is-active { background: var(--pink-100, #ffe6f0); color: var(--pink-700, #a81b5d); }
.tci-empty { padding: 8px 10px; font-size: 12.5px; color: var(--muted, #6b7280); }
</style>
