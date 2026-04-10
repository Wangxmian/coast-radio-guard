<template>
  <div class="page-container">
    <slot />
  </div>
</template>

<style scoped>
.page-container {
  width: min(1320px, 100%);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: var(--crg-space-4);
}
</style>
