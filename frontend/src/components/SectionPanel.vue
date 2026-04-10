<template>
  <el-card class="section-panel" :body-style="{ padding: bodyPadding }">
    <template #header>
      <div class="panel-header">
        <div>
          <h3 class="panel-title">{{ title }}</h3>
          <p v-if="subtitle" class="panel-subtitle">{{ subtitle }}</p>
        </div>
        <div v-if="$slots.actions" class="panel-actions">
          <slot name="actions" />
        </div>
      </div>
    </template>
    <slot />
  </el-card>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  bodyPadding: {
    type: String,
    default: '16px'
  }
})
</script>

<style scoped>
.section-panel {
  border-radius: var(--crg-radius-md);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.panel-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--crg-text-main);
}

.panel-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--crg-text-muted);
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
