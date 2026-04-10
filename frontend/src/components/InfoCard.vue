<template>
  <el-card class="info-card" :body-style="{ padding: '18px 18px 16px' }">
    <div class="head">
      <span class="label">{{ label }}</span>
      <span class="dot" :class="toneClass"></span>
    </div>
    <div class="value">{{ value }}</div>
    <div v-if="hint" class="hint">{{ hint }}</div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    default: '-'
  },
  hint: {
    type: String,
    default: ''
  },
  tone: {
    type: String,
    default: 'default'
  }
})

const toneClass = computed(() => `tone-${props.tone}`)
</script>

<style scoped>
.info-card {
  height: 100%;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.label {
  font-size: 13px;
  color: var(--crg-text-secondary);
  font-weight: 600;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #95a4b4;
}

.value {
  margin-top: 14px;
  font-size: 32px;
  line-height: 1;
  font-weight: 700;
  color: var(--crg-text-main);
}

.hint {
  margin-top: 12px;
  font-size: 12px;
  color: var(--crg-text-muted);
}

.tone-primary {
  background: #3f7aa6;
}

.tone-danger {
  background: #c63636;
}

.tone-warning {
  background: #d3861f;
}

.tone-success {
  background: #1e8e5a;
}
</style>
