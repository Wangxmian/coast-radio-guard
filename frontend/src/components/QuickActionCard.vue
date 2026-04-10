<template>
  <button class="quick-card" @click="$emit('select')">
    <h4>{{ title }}</h4>
    <p>{{ desc }}</p>
  </button>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  desc: {
    type: String,
    default: ''
  }
})

defineEmits(['select'])
</script>

<style scoped>
.quick-card {
  width: 100%;
  border: 1px solid var(--crg-border-color);
  border-radius: 10px;
  background: #fff;
  text-align: left;
  padding: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.quick-card:hover {
  border-color: #aac6dc;
  background: #f6fbff;
}

h4 {
  margin: 0;
  font-size: 14px;
  color: var(--crg-text-main);
}

p {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--crg-text-muted);
  line-height: 1.4;
}
</style>
