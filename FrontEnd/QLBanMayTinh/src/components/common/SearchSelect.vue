<template>
  <!-- Combobox tùy biến — thay <select> gốc trình duyệt (luôn trắng, không theo theme,
       cắt chữ dài) bằng dropdown nổi theo theme app, có ô tìm kiếm khi danh sách dài.
       Panel teleport ra <body> + position:fixed — nếu không, panel bị modal cha
       (overflow-y-auto) cắt cụt khi nằm gần đáy vùng cuộn. -->
  <div class="position-relative" ref="rootEl">
    <button type="button" class="form-select form-select-sm text-start d-flex align-items-center"
            :disabled="disabled"
            :class="{ 'text-secondary': !selectedLabel }"
            style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong);"
            role="combobox" aria-haspopup="listbox" :aria-expanded="open"
            @click="toggle" @keydown="onTriggerKeydown">
      <span class="text-truncate">{{ selectedLabel || placeholder }}</span>
    </button>

    <Teleport to="body">
      <div v-if="open" ref="panelEl" class="rounded-3 d-flex flex-column"
           :style="{ position: 'fixed', top: panelPos.top + 'px', left: panelPos.left + 'px',
                     minWidth: panelPos.width + 'px', width: 'max-content', maxWidth: '340px',
                     background: 'var(--bg-card)', border: '1px solid var(--border-color-strong)',
                     boxShadow: '0 12px 32px rgba(0,0,0,0.4)', zIndex: 2000 }">
        <input v-if="options.length > 6" v-model="query" ref="searchEl"
               class="form-control form-control-sm m-2"
               style="width:auto;background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong);"
               :placeholder="t('common.searchPlaceholder')" @click.stop @keydown="onTriggerKeydown" />
        <div class="overflow-y-auto" role="listbox" style="max-height:240px;">
          <div v-for="(opt, idx) in filteredOptions" :key="opt.value" ref="optionEls"
               class="px-3 py-2 small" role="option" :aria-selected="opt.value === modelValue"
               style="cursor:pointer;white-space:normal;"
               :style="(opt.value === modelValue || idx === highlightedIndex) ? { background: 'var(--accent)', color: 'var(--accent-text)' } : {}"
               @mouseenter="highlightedIndex = idx"
               @click="select(opt.value)">
            {{ opt.label }}
          </div>
          <div v-if="filteredOptions.length === 0" class="px-3 py-3 text-secondary small text-center">{{ t('common.noResults') }}</div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick } from 'vue';
import { t } from '../../i18n/index.js';

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  options:    { type: Array,   required: true }, // [{ value, label }]
  placeholder:{ type: String,  default: '' },
  disabled:   { type: Boolean, default: false },
});
const emit = defineEmits(['update:modelValue']);

const open = ref(false);
const query = ref('');
const rootEl = ref(null);
const panelEl = ref(null);
const searchEl = ref(null);
const optionEls = ref([]);
const highlightedIndex = ref(-1);
const panelPos = reactive({ top: 0, left: 0, width: 0 });

const selectedLabel = computed(() => props.options.find(o => o.value === props.modelValue)?.label ?? '');
const filteredOptions = computed(() => {
  if (!query.value) return props.options;
  const q = query.value.toLowerCase();
  return props.options.filter(o => o.label.toLowerCase().includes(q));
});
// Gõ tìm lọc lại danh sách — chỉ số đang bôi đen của lần gõ trước không còn đúng nữa.
watch(filteredOptions, () => {
  highlightedIndex.value = filteredOptions.value.findIndex(o => o.value === props.modelValue);
});

const updatePanelPos = () => {
  const r = rootEl.value?.getBoundingClientRect();
  if (!r) return;
  panelPos.top = r.bottom + 4;
  panelPos.left = r.left;
  panelPos.width = r.width;
};

const toggle = () => {
  if (props.disabled) return;
  open.value = !open.value;
  if (open.value) {
    query.value = '';
    highlightedIndex.value = filteredOptions.value.findIndex(o => o.value === props.modelValue);
    updatePanelPos();
    nextTick(() => searchEl.value?.focus());
  }
};
const select = (value) => {
  emit('update:modelValue', value);
  open.value = false;
  rootEl.value?.querySelector('button')?.focus();
};

// Điều hướng bàn phím — trước đây chỉ bấm chuột chọn được, không thao tác được bằng
// bàn phím (Tab vào nút thì mở/đóng được nhờ semantics <button> gốc, nhưng vào trong danh
// sách option thì bó tay vì các dòng chỉ là <div> thường, không nhận phím mũi tên/Enter).
const moveHighlight = (delta) => {
  const len = filteredOptions.value.length;
  if (len === 0) return;
  highlightedIndex.value = (highlightedIndex.value + delta + len) % len;
  nextTick(() => optionEls.value[highlightedIndex.value]?.scrollIntoView({ block: 'nearest' }));
};
const onTriggerKeydown = (e) => {
  if (!open.value) {
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp' || e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      toggle();
    }
    return;
  }
  if (e.key === 'ArrowDown') { e.preventDefault(); moveHighlight(1); }
  else if (e.key === 'ArrowUp') { e.preventDefault(); moveHighlight(-1); }
  else if (e.key === 'Enter') {
    e.preventDefault();
    const opt = filteredOptions.value[highlightedIndex.value];
    if (opt) select(opt.value);
  } else if (e.key === 'Escape') {
    e.preventDefault();
    open.value = false;
    rootEl.value?.querySelector('button')?.focus();
  } else if (e.key === 'Tab') {
    open.value = false;
  }
};

// Modal cha cuộn được trong lúc panel mở → toa do cu thanh sai vi tri, dong panel cho don gian.
// Nhưng cuộn BÊN TRONG panel (danh sách option dài) không được tính — đó là scroll bình
// thường của người dùng, không phải scroll của modal cha.
const closeOnScroll = (e) => {
  if (panelEl.value?.contains(e.target)) return;
  open.value = false;
};
const onDocClick = (e) => {
  if (!open.value) return;
  if (rootEl.value?.contains(e.target)) return;
  if (panelEl.value?.contains(e.target)) return; // panel teleport ra <body>, khong nam trong rootEl
  open.value = false;
};
watch(open, (isOpen) => {
  if (isOpen) {
    document.addEventListener('mousedown', onDocClick);
    window.addEventListener('scroll', closeOnScroll, true);
    window.addEventListener('resize', closeOnScroll);
  } else {
    document.removeEventListener('mousedown', onDocClick);
    window.removeEventListener('scroll', closeOnScroll, true);
    window.removeEventListener('resize', closeOnScroll);
  }
});
watch(() => props.disabled, (d) => { if (d) open.value = false; });
</script>
