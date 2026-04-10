<template>
  <SectionPanel :title="title" :subtitle="subtitle" :body-padding="bodyPadding">
    <template #actions>
      <slot name="actions" />
    </template>
    <slot />
  </SectionPanel>
</template>

<script setup>
import SectionPanel from './SectionPanel.vue'

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
    default: '14px'
  }
})
</script>
