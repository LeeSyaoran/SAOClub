<script setup>
defineProps({
  rows: { type: Number, default: 3 },
  height: { type: String, default: "20px" },
});
</script>

<template>
  <div class="d-flex flex-column gap-2 py-3">
    <div
      v-for="i in rows"
      :key="i"
      class="rounded-3 placeholder-glow"
    >
      <span
        class="placeholder w-100 d-block rounded-3"
        :style="{ height, background: 'var(--bg-hover)' }"
      ></span>
    </div>
  </div>
</template>
