<template>
  <span class="status-tag" :class="`status-${normalized}`">{{ normalized }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: {
    type: String,
    default: ''
  }
})

const normalized = computed(() => {
  const v = String(props.status || '').trim().toUpperCase()
  return v || 'UNKNOWN'
})
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid;
  font-size: 12px;
  font-weight: 600;
}

.status-tag::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-WAITING {
  color: #5f6d7e;
  border-color: #cfd8e3;
  background: #f5f8fc;
}

.status-PROCESSING {
  color: #1e6fb4;
  border-color: #b7d5f1;
  background: #eef6fd;
}

.status-PENDING {
  color: #7b5eb1;
  border-color: #d5c9ed;
  background: #f6f1ff;
}

.status-SUCCESS,
.status-ENABLED,
.status-1 {
  color: #1f7b4d;
  border-color: #b9e3cb;
  background: #f0fbf5;
}

.status-FAILED,
.status-DISABLED,
.status-0,
.status-ERROR,
.status-HIGH {
  color: #b42323;
  border-color: #f1b5b5;
  background: #fff2f2;
}

.status-UNHANDLED,
.status-MEDIUM {
  color: #a86414;
  border-color: #f3d1a7;
  background: #fff8ef;
}

.status-HANDLED,
.status-LOW {
  color: #1f7b4d;
  border-color: #b9e3cb;
  background: #f0fbf5;
}

.status-UNKNOWN {
  color: #5f6d7e;
  border-color: #cfd8e3;
  background: #f5f8fc;
}
</style>
