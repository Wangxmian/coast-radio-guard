<template>
  <div class="page-header">
    <div class="left">
      <h2 class="title">{{ title }}</h2>
      <p v-if="description" class="desc">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--crg-space-3);
}

.title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.2px;
  color: var(--crg-text-main);
}

.desc {
  margin: 8px 0 0;
  color: var(--crg-text-secondary);
  font-size: 14px;
}

.actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

@media (max-width: 760px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
