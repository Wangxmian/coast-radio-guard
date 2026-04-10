<template>
  <div class="timeline-wrap">
    <template v-if="items.length">
      <div v-for="item in items" :key="item.id" class="timeline-item">
        <div class="timeline-time">{{ item.time }}</div>
        <div class="timeline-dot"></div>
        <div class="timeline-content">{{ item.content }}</div>
      </div>
    </template>
    <EmptyState v-else :description="emptyText" />
  </div>
</template>

<script setup>
import EmptyState from './EmptyState.vue'

defineProps({
  items: {
    type: Array,
    default: () => []
  },
  emptyText: {
    type: String,
    default: '暂无时间轴数据'
  }
})
</script>

<style scoped>
.timeline-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 132px 16px 1fr;
  gap: 10px;
  align-items: start;
}

.timeline-time {
  color: var(--crg-text-muted);
  font-size: 12px;
  line-height: 1.45;
  white-space: pre-line;
  word-break: keep-all;
}

.timeline-dot {
  width: 10px;
  height: 10px;
  margin-top: 3px;
  border-radius: 50%;
  border: 2px solid #6c9cc2;
  background: #f3f8fc;
}

.timeline-content {
  border-radius: 8px;
  border: 1px solid var(--crg-border-color);
  background: #f8fbfe;
  padding: 8px 10px;
  font-size: 13px;
  line-height: 1.45;
  color: var(--crg-text-main);
  word-break: break-word;
}

@media (max-width: 700px) {
  .timeline-item {
    grid-template-columns: 1fr;
  }

  .timeline-dot {
    display: none;
  }
}
</style>
