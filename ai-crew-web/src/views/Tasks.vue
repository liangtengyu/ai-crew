<template>
  <div>
    <div class="page-card">
      <div class="toolbar"><h3>🧑‍💼 BOSS · 发布新需求</h3></div>
      <el-form label-width="90px">
        <el-form-item label="需求标题">
          <el-input v-model="form.title" maxlength="100" placeholder="一句话说清要做什么，如：做一个员工请假审批小程序" />
        </el-form-item>
        <el-form-item label="需求描述">
          <el-input v-model="form.requirement" type="textarea" :rows="5" maxlength="5000" show-word-limit
            placeholder="把你的需求原话写在这里，越具体，团队的方案越靠谱…" />
        </el-form-item>
        <el-form-item label="协作剧本">
          <el-select v-model="form.playbookId" style="width: 320px">
            <el-option v-for="p in playbooks" :key="p.id" :value="p.id"
              :label="`${p.name}（${(p.steps || []).length} 个环节）`" />
          </el-select>
          <span style="margin-left: 12px; color: #909399; font-size: 13px">
            团队 = 剧本各环节的执行角色，可到【协作剧本】调整
          </span>
        </el-form-item>
        <el-form-item label="">
          <el-checkbox v-model="form.start">发布后立即启动协作</el-checkbox>
          <el-button type="primary" :loading="submitting" style="margin-left: 20px" @click="submit">发布需求</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card">
      <div class="toolbar"><h3>全部需求</h3></div>
      <el-table :data="tasks">
        <el-table-column prop="id" label="#" width="60" />
        <el-table-column prop="title" label="需求" min-width="240" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }"><status-tag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="轮次" width="80">
          <template #default="{ row }">第 {{ row.round }} 轮</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/tasks/${row.id}`)">作战室</el-button>
            <el-popconfirm title="删除该需求及全部协作记录？" @confirm="del(row)">
              <template #reference><el-button link type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { fmtTime } from '../utils/md'
import StatusTag from '../components/StatusTag.vue'

const router = useRouter()
const form = reactive({ title: '', requirement: '', playbookId: null, start: true })
const playbooks = ref([])
const tasks = ref([])
const submitting = ref(false)

async function load() {
  try {
    tasks.value = await api.listTasks()
    playbooks.value = await api.listPlaybooks()
    if (!form.playbookId && playbooks.value.length) form.playbookId = playbooks.value[0].id
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function submit() {
  if (!form.title.trim() || !form.requirement.trim()) {
    ElMessage.warning('请填写需求标题和描述')
    return
  }
  submitting.value = true
  try {
    const id = await api.createTask({ ...form })
    ElMessage.success('需求已发布')
    router.push(`/tasks/${id}`)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    submitting.value = false
  }
}

async function del(row) {
  try {
    await api.deleteTask(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>
