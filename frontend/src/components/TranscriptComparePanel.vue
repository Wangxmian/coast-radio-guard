<template>
  <div class="compare-panel">
    <div class="compare-meta">
      <el-tag size="small" type="info">原始 ASR</el-tag>
      <el-tag size="small" :type="fallback ? 'warning' : 'success'">
        {{ fallback ? '纠错回退' : 'LLM 纠错' }}
      </el-tag>
      <span v-if="provider" class="compare-provider">provider: {{ provider }}</span>
    </div>

    <div class="compare-block">
      <div class="compare-label">原始文本</div>
      <div class="compare-text">{{ rawText || '-' }}</div>
    </div>

    <div class="compare-block">
      <div class="compare-label">纠错文本</div>
      <div class="compare-text compare-text--corrected">{{ correctedText || rawText || '-' }}</div>
    </div>

    <div class="compare-block" v-if="diffSegments.length">
      <div class="compare-label">差异对照</div>
      <div class="diff-text">
        <template v-for="(seg, idx) in diffSegments" :key="idx">
          <span v-if="seg.type === 'same'">{{ seg.text }}</span>
          <span v-else-if="seg.type === 'replace'" class="diff-replace">{{ seg.from }} -> {{ seg.to }}</span>
          <span v-else-if="seg.type === 'insert'" class="diff-insert">{{ seg.text }}</span>
          <span v-else-if="seg.type === 'delete'" class="diff-delete">{{ seg.text }}</span>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  rawText: {
    type: String,
    default: ''
  },
  correctedText: {
    type: String,
    default: ''
  },
  correctionDiff: {
    type: String,
    default: ''
  },
  provider: {
    type: String,
    default: ''
  },
  fallback: {
    type: Boolean,
    default: false
  }
})

const diffSegments = computed(() => {
  if (!props.correctionDiff) return []
  try {
    const parsed = JSON.parse(props.correctionDiff)
    return Array.isArray(parsed) ? parsed : []
  } catch (_e) {
    return []
  }
})
</script>

<style scoped>
.compare-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.compare-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.compare-provider {
  font-size: 12px;
  color: #64748b;
}

.compare-block {
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #e8edf5;
  background: #f8fafc;
}

.compare-label {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 8px;
}

.compare-text {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #0f172a;
}

.compare-text--corrected {
  color: #14532d;
}

.diff-text {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #0f172a;
}

.diff-replace {
  display: inline-block;
  margin: 0 1px;
  padding: 0 4px;
  border-radius: 6px;
  background: #fff7ed;
  color: #c2410c;
  font-weight: 700;
}

.diff-insert {
  display: inline-block;
  margin: 0 1px;
  padding: 0 4px;
  border-radius: 6px;
  background: #ecfdf5;
  color: #166534;
}

.diff-delete {
  display: inline-block;
  margin: 0 1px;
  padding: 0 4px;
  border-radius: 6px;
  background: #fff1f2;
  color: #b91c1c;
  text-decoration: line-through;
}
</style>
