<template>
  <div class="chat-item" :class="roleClass">
    <div class="bubble">
      <div class="meta">{{ roleText }}</div>
      <div class="content markdown-body" v-html="renderedContent"></div>
      <div v-if="showMetaTags" class="message-tags">
        <span v-if="intent" class="tag">意图：{{ intent }}</span>
        <span v-if="source" class="tag">来源：{{ sourceLabel }}</span>
        <span v-if="fallback" class="tag tag--warn">已降级</span>
      </div>
      <details v-if="hasStructuredData" class="structured-block">
        <summary>查看数据详情</summary>
        <pre>{{ structuredText }}</pre>
      </details>
      <div v-if="time" class="time">{{ time }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  role: {
    type: String,
    default: 'assistant'
  },
  content: {
    type: String,
    default: ''
  },
  intent: {
    type: String,
    default: ''
  },
  source: {
    type: String,
    default: ''
  },
  fallback: {
    type: Boolean,
    default: false
  },
  structuredData: {
    type: [Object, Array, String, Number, Boolean],
    default: null
  },
  time: {
    type: String,
    default: ''
  }
})

const roleClass = computed(() => `role-${props.role}`)
const roleText = computed(() => {
  if (props.role === 'user') return '你'
  if (props.role === 'system') return '系统'
  return '智能体'
})

const hasStructuredData = computed(() => props.structuredData !== null && props.structuredData !== undefined)
const showMetaTags = computed(() => props.role === 'assistant' && (props.intent || props.source || props.fallback))
const sourceLabel = computed(() => formatSource(props.source))
const structuredText = computed(() => {
  if (!hasStructuredData.value) return ''
  if (typeof props.structuredData === 'string') {
    return props.structuredData
  }
  try {
    return JSON.stringify(props.structuredData, null, 2)
  } catch (_e) {
    return String(props.structuredData)
  }
})

const renderedContent = computed(() => renderMarkdown(props.content || ''))

function renderMarkdown(text) {
  const lines = String(text || '').replace(/\r\n/g, '\n').split('\n')
  const html = []
  let paragraph = []
  let unordered = []
  let ordered = []

  const flushParagraph = () => {
    if (!paragraph.length) return
    html.push(`<p>${paragraph.map(renderInline).join('<br>')}</p>`)
    paragraph = []
  }

  const flushUnordered = () => {
    if (!unordered.length) return
    html.push(`<ul>${unordered.map((item) => `<li>${renderInline(item)}</li>`).join('')}</ul>`)
    unordered = []
  }

  const flushOrdered = () => {
    if (!ordered.length) return
    html.push(`<ol>${ordered.map((item) => `<li>${renderInline(item)}</li>`).join('')}</ol>`)
    ordered = []
  }

  for (const rawLine of lines) {
    const line = rawLine.trimEnd()
    const trimmed = line.trim()

    if (!trimmed) {
      flushParagraph()
      flushUnordered()
      flushOrdered()
      continue
    }

    const headingMatch = trimmed.match(/^(#{1,3})\s+(.+)$/)
    if (headingMatch) {
      flushParagraph()
      flushUnordered()
      flushOrdered()
      const level = headingMatch[1].length
      html.push(`<h${level}>${renderInline(headingMatch[2])}</h${level}>`)
      continue
    }

    const orderedMatch = trimmed.match(/^\d+\.\s+(.+)$/)
    if (orderedMatch) {
      flushParagraph()
      flushUnordered()
      ordered.push(orderedMatch[1])
      continue
    }

    const unorderedMatch = trimmed.match(/^[-*]\s+(.+)$/)
    if (unorderedMatch) {
      flushParagraph()
      flushOrdered()
      unordered.push(unorderedMatch[1])
      continue
    }

    flushUnordered()
    flushOrdered()
    paragraph.push(trimmed)
  }

  flushParagraph()
  flushUnordered()
  flushOrdered()

  return html.join('')
}

function renderInline(text) {
  let html = escapeHtml(text)
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*([^*\n]+)\*/g, '<em>$1</em>')
  return html
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function formatSource(source) {
  if (!source) return ''
  if (source === 'data') return '数据库'
  if (source === 'llm') return '大模型'
  if (source === 'fallback') return '系统兜底'
  return source
}
</script>

<style scoped>
.chat-item {
  display: flex;
}

.role-user {
  justify-content: flex-end;
}

.role-assistant,
.role-system {
  justify-content: flex-start;
}

.bubble {
  max-width: min(760px, 90%);
  border-radius: 12px;
  padding: 10px 12px;
  border: 1px solid var(--crg-border-color);
  background: #f8fbfe;
}

.role-user .bubble {
  background: #eaf2f8;
  border-color: #c8dbe9;
}

.meta {
  font-size: 12px;
  color: var(--crg-text-muted);
  margin-bottom: 4px;
}

.content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--crg-text-main);
}

.content :deep(p) {
  margin: 0 0 12px;
}

.content :deep(p:last-child) {
  margin-bottom: 0;
}

.content :deep(h1),
.content :deep(h2),
.content :deep(h3) {
  margin: 0 0 12px;
  font-size: 16px;
  line-height: 1.5;
}

.content :deep(ul),
.content :deep(ol) {
  margin: 0 0 12px;
  padding-left: 22px;
}

.content :deep(li) {
  margin: 4px 0;
}

.content :deep(strong) {
  font-weight: 700;
}

.content :deep(em) {
  font-style: italic;
}

.content :deep(code) {
  padding: 1px 6px;
  border-radius: 6px;
  background: #eef2f7;
  font-size: 13px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.message-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eef2ff;
  color: #3730a3;
  font-size: 11px;
  font-weight: 600;
}

.tag--warn {
  background: #fff7ed;
  color: #c2410c;
}

.structured-block {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #d7e2ee;
}

.structured-block summary {
  cursor: pointer;
  font-size: 12px;
  color: #475569;
  font-weight: 600;
}

.structured-block pre {
  margin: 10px 0 0;
  padding: 10px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #0f172a;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.6;
}

.time {
  margin-top: 6px;
  font-size: 11px;
  color: #90a0b0;
}
</style>
