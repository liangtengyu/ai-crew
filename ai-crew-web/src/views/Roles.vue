<template>
  <div class="page-card">
    <div class="toolbar">
      <h3>👥 角色管理</h3>
      <el-button type="primary" @click="open()">+ 新增角色</el-button>
    </div>
    <el-alert type="info" :closable="false" style="margin-bottom: 12px"
      title="每个角色绑定一个模型即可实现「A 模型当产品经理、B 模型当开发」；可以随意增加 C、D、E 等新角色（测试、架构师、数据分析师…），再到【协作剧本】把他们加入流程。人设留空将使用内置默认人设。" />
    <el-table :data="list">
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <span class="avatar-mini" :style="{ background: row.color || '#909399' }">{{ row.emoji || '🤖' }}</span>
          <b style="margin-left: 8px">{{ row.name }}</b>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ typeLabel(row.roleType) }}</template>
      </el-table-column>
      <el-table-column prop="modelName" label="绑定模型" min-width="180" />
      <el-table-column label="自定义人设" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.systemPrompt" size="small">有</el-tag>
          <el-tag v-else size="small" type="info">默认</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80">
        <template #default="{ row }"><el-switch v-model="row.enabled" @change="save(row)" /></template>
      </el-table-column>
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-popconfirm title="删除该角色？" @confirm="del(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑角色' : '新增角色'" width="600px">
      <el-form label-width="100px">
        <el-form-item label="角色名" required>
          <el-input v-model="form.name" placeholder="如：数据分析师·小王" />
        </el-form-item>
        <el-form-item label="角色类型" required>
          <el-select v-model="form.roleType" style="width: 100%">
            <el-option v-for="t in types" :key="t.value" :value="t.value" :label="t.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="头像 / 颜色">
          <el-input v-model="form.emoji" style="width: 90px" placeholder="🧭" />
          <el-color-picker v-model="form.color" style="margin-left: 10px" />
        </el-form-item>
        <el-form-item label="绑定模型" required>
          <el-select v-model="form.modelConfigId" style="width: 100%">
            <el-option v-for="m in models" :key="m.id" :value="m.id" :label="`${m.name}（${m.model}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="自定义人设">
          <el-input v-model="form.systemPrompt" type="textarea" :rows="5"
            placeholder="留空使用内置默认人设；也可写：你是「AI 虚拟团队」中的资深数据分析师，擅长…" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const types = [
  { value: 'PM', label: '产品经理' },
  { value: 'DEV', label: '开发工程师' },
  { value: 'TEST', label: '测试工程师' },
  { value: 'ARCHITECT', label: '架构师' },
  { value: 'BA', label: '业务分析师' },
  { value: 'UI', label: 'UI 设计师' },
  { value: 'CUSTOM', label: '自定义角色' }
]
const typeLabel = (v) => types.find((t) => t.value === v)?.label || v

const list = ref([])
const models = ref([])
const dialog = ref(false)
const form = reactive({})

async function load() {
  try {
    const [roles, ms] = await Promise.all([api.listRoles(), api.listModels()])
    list.value = roles
    models.value = ms
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function open(row) {
  Object.assign(form, row || {
    name: '', roleType: 'CUSTOM', emoji: '', color: '#909399',
    systemPrompt: '', modelConfigId: null, enabled: true
  })
  dialog.value = true
}

async function save(row) {
  const target = row?.id ? row : form
  try {
    if (target.id) await api.updateRole(target.id, target)
    else await api.createRole(target)
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function del(row) {
  try {
    await api.deleteRole(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.avatar-mini {
  display: inline-flex; width: 26px; height: 26px; border-radius: 50%;
  align-items: center; justify-content: center; color: #fff; font-size: 14px;
  vertical-align: middle;
}
</style>
