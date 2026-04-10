<template>
  <div class="json-panel">
    <div class="tools">
      <el-segmented v-model="mode" :options="modeOptions" />
      <el-button size="small" @click="copyJson">复制 JSON</el-button>
    </div>

    <template v-if="mode === 'friendly'">
      <el-scrollbar max-height="340px" class="friendly-wrap">
        <slot name="friendly" />
      </el-scrollbar>
    </template>

    <template v-else>
      <el-scrollbar max-height="340px" class="json-wrap">
        <pre>{{ prettyJson }}</pre>
      </el-scrollbar>
    </template>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  json: {
    type: [Object, Array, String, Number, Boolean],
    default: () => ({})
  }
})

const mode = ref('friendly')
const modeOptions = [
  { label: '友好视图', value: 'friendly' },
  { label: '原始 JSON', value: 'json' }
]

const prettyJson = computed(() => {
  if (typeof props.json === 'string') return props.json
  try {
    return JSON.stringify(props.json || {}, null, 2)
  } catch (_e) {
    return '{}'
  }
})

async function copyJson() {
  try {
    await navigator.clipboard.writeText(prettyJson.value)
    ElMessage.success('JSON 已复制')
  } catch (_e) {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
.json-panel {
  border: 1px solid var(--crg-border-color);
  border-radius: 10px;
  overflow: hidden;
}

.tools {
  padding: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--crg-border-color);
  background: #f8fbfe;
}

.friendly-wrap,
.json-wrap {
  padding: 10px;
}

pre {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: #2f4357;
}
</style>
