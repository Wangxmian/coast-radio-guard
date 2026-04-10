<template>
  <span class="risk-tag" :class="`risk-${normalized}`">{{ normalized }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  level: {
    type: String,
    default: ''
  }
})

const normalized = computed(() => {
  const v = String(props.level || '').trim().toUpperCase()
  if (v === 'HIGH' || v === 'MEDIUM' || v === 'LOW') return v
  return v || 'UNKNOWN'
})
</script>

<style scoped>
.risk-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.2px;
}

.risk-HIGH {
  color: #b42323;
  border-color: #f1b5b5;
  background: #fff2f2;
}

.risk-MEDIUM {
  color: #a86414;
  border-color: #f3d1a7;
  background: #fff8ef;
}

.risk-LOW {
  color: #1f7b4d;
  border-color: #b9e3cb;
  background: #f0fbf5;
}

.risk-UNKNOWN {
  color: #5f6d7e;
  border-color: #cdd6e0;
  background: #f4f7fa;
}
</style>
