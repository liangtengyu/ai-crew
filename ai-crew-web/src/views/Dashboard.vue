<template>
  <div>
    <div class="toolbar">
      <h3>📊 概览</h3>
      <el-button type="primary" @click="$router.push('/tasks')">发布新需求</el-button>
    </div>

    <el-row :gutter="16">
      <el-col :span="4" v-for="c in cards" :key="c.label">
        <div class="page-card" style="text-align: center; padding: 14px">
          <div style="font-size: 26px; font-weight: 700" :style="{ color: c.color }">{{ c.value }}</div>
          <div style="font-size: 12px; color: #909399; margin-top: 4px">{{ c.label }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card">
      <div class="toolbar"><h3>协作流程</h3></div>
      <el-steps :active="4" align-center>
        <el-step title="需求分析" description="产品经理理解并澄清需求" />
        <el-step title="方案设计" description="输出方案与开发任务清单" />
        <el-step title="开发实现" description="开发工程师实现并提交" />
        <el-step title="验收评审" description="产品经理把关，不通过打回返工" />
        <el-step title="交付 BOSS" description="交付总结，BOSS 采纳 / 退回" />
      </el-steps>
      <el-alert type="info" :closable="false" style="margin-top: 14px"
        title="开始前请先到【模型配置】填写各模型的 apiKey，再到【角色管理】把角色绑定到不同模型（如 A 模型当产品经理、B 模型当开发），即可在【需求大厅】发布需求观看团队协作。" />
    </div>

    <div class="page-card">
      <div class="toolbar"><h3>最近需求</h3><el-button link type="primary" @click="$router.push('/tasks')">全部 →</el-button></div>
      <el-table :data="recent" size="small">
        <el-table-column prop="title" label="需求" min-width="220" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><status-tag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="轮次" width="70">
          <template #default="{ row }">第 {{ row.round }} 轮</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/tasks/${row.id}`)">进入作战室</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { fmtTime } from '../utils/md'
import StatusTag from '../components/StatusTag.vue'

const stats = ref({})
const tasks = ref([])

const cards = computed(() => [
  { label: '需求总数', value: stats.value.taskTotal ?? '-', color: '#303133' },
  { label: '协作中', value: stats.value.taskRunning ?? '-', color: '#e6a23c' },
  { label: '待验收', value: stats.value.taskDelivered ?? '-', color: '#409eff' },
  { label: '已采纳', value: stats.value.taskAccepted ?? '-', color: '#67c23a' },
  { label: 'AI 角色', value: stats.value.roleTotal ?? '-', color: '#909399' },
  { label: '已配模型', value: stats.value.modelTotal ?? '-', color: '#909399' }
])

const recent = computed(() => tasks.value.slice(0, 5))

onMounted(async () => {
  try {
    stats.value = await api.stats()
    tasks.value = await api.listTasks()
  } catch (e) {
    ElMessage.error(e.message)
  }
})
</script>
