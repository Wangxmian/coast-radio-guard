<template>
  <dl class="kv-list">
    <div v-for="item in items" :key="item.key" class="kv-item">
      <dt>{{ item.label }}</dt>
      <dd :title="String(item.value ?? '-')">{{ item.value ?? '-' }}</dd>
    </div>
  </dl>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.kv-list {
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.kv-item {
  display: grid;
  grid-template-columns: 108px 1fr;
  gap: 12px;
}

dt {
  margin: 0;
  color: var(--crg-text-muted);
  font-size: 13px;
}

dd {
  margin: 0;
  color: var(--crg-text-main);
  font-size: 14px;
  line-height: 1.45;
  word-break: break-all;
}

@media (max-width: 720px) {
  .kv-item {
    grid-template-columns: 1fr;
    gap: 4px;
  }
}
</style>
