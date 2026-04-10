<template>
  <div class="empty-wrap">
    <el-empty :description="description" :image-size="86" />
  </div>
</template>

<script setup>
defineProps({
  description: {
    type: String,
    default: '暂无数据'
  }
})
</script>

<style scoped>
.empty-wrap {
  display: flex;
  justify-content: center;
  padding: 16px 0;
}
</style>
