<template>
  <el-tag :type="map[status]?.type || 'info'" effect="light">{{ map[status]?.label || status }}</el-tag>
</template>

<script setup>
defineProps({ status: String })

const map = {
  CREATED: { label: '待启动', type: 'info' },
  RUNNING: { label: '协作中', type: 'warning' },
  DELIVERED: { label: '待 BOSS 验收', type: 'primary' },
  ACCEPTED: { label: '已采纳', type: 'success' },
  FAILED: { label: '已中断', type: 'danger' }
}
</script>
