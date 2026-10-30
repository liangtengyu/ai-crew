<template>
  <div v-for="pb in playbooks" :key="pb.id" class="page-card">
    <div class="toolbar">
      <h3>🎬 {{ pb.name }}</h3>
      <div>
        <el-button @click="editPb(pb)">编辑剧本信息</el-button>
        <el-button type="primary" @click="openStep(pb, null)">+ 添加环节</el-button>
      </div>
    </div>
    <p style="color: #909399; font-size: 13px; margin: 0 0 12px">
      {{ pb.description || '（无描述）' }} · 最大返工轮数：{{ pb.maxRounds }} · 环节按顺序执行，验收关卡不通过时打回返工
    </p>
    <el-table :data="pb.steps">
      <el-table-column label="顺序" width="70">
        <template #default="{ row }">{{ row.stepOrder }}</template>
      </el-table-column>
      <el-table-column prop="name" label="环节" min-width="120" />
      <el-table-column label="执行角色" min-width="160">
        <template #default="{ row }">{{ row.emoji }} {{ row.actorName }}</template>
      </el-table-column>
      <el-table-column label="验收关卡" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.requireVerdict" type="warning" size="small">是</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="驳回跳转" width="90">
        <template #default="{ row }">{{ row.onRejectOrder ? `第 ${row.onRejectOrder} 步` : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openStep(pb, row)">编辑</el-button>
          <el-button link @click="move(row, 'up')">上移</el-button>
          <el-button link @click="move(row, 'down')">下移</el-button>
          <el-popconfirm title="删除该环节？" @confirm="del(pb, row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="pbDialog" title="编辑剧本信息" width="480px">
      <el-form label-width="110px">
        <el-form-item label="剧本名称"><el-input v-model="pbForm.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="pbForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="最大返工轮数">
          <el-input-number v-model="pbForm.maxRounds" :min="1" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pbDialog = false">取消</el-button>
        <el-button type="primary" @click="savePb">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stepDialog" :title="stepForm.stepId ? '编辑环节' : '添加环节'" width="640px">
      <el-form label-width="110px">
        <el-form-item label="环节名称" required>
          <el-input v-model="stepForm.name" placeholder="如：数据校验评审" />
        </el-form-item>
        <el-form-item label="环节编码">
          <el-input v-model="stepForm.code" placeholder="可选，如 DATA_REVIEW；留空自动生成" />
        </el-form-item>
        <el-form-item label="执行角色" required>
          <el-select v-model="stepForm.actorRoleId" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :value="r.id" :label="`${r.emoji} ${r.name}（${typeLabel(r.roleType)} · ${r.modelName}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="环节指令" required>
          <el-input v-model="stepForm.promptTemplate" type="textarea" :rows="7"
            placeholder="告诉该角色本环节要做什么。支持占位符：{title} 需求标题、{requirement} 需求原文、{history} 协作历史、{round}/{maxRounds} 轮次" />
        </el-form-item>
        <el-form-item label="验收关卡">
          <el-switch v-model="stepForm.requireVerdict" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px">开启后该角色必须给出 通过 / 打回 结论</span>
        </el-form-item>
        <el-form-item v-if="stepForm.requireVerdict" label="驳回跳转到">
          <el-select v-model="stepForm.onRejectOrder" style="width: 200px">
            <el-option v-for="s in pb.steps" :key="s.stepOrder" :value="s.stepOrder" :label="`第 ${s.stepOrder} 步 · ${s.name}`" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stepDialog = false">取消</el-button>
        <el-button type="primary" @click="saveStep">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const playbooks = ref([])
const roles = ref([])
const pbDialog = ref(false)
const stepDialog = ref(false)
const pbForm = reactive({})
const stepForm = reactive({})

const types = { PM: '产品经理', DEV: '开发', TEST: '测试', ARCHITECT: '架构师', BA: '业务分析', UI: 'UI 设计', CUSTOM: '自定义' }
const typeLabel = (v) => types[v] || v

async function load() {
  try {
    playbooks.value = await api.listPlaybooks()
    roles.value = await api.listRoles()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function editPb(pb) {
  Object.assign(pbForm, { id: pb.id, name: pb.name, description: pb.description, maxRounds: pb.maxRounds })
  pbDialog.value = true
}

async function savePb() {
  try {
    await api.updatePlaybook(pbForm.id, { ...pbForm })
    ElMessage.success('已保存')
    pbDialog.value = false
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openStep(pb, step) {
  Object.assign(stepForm, step
    ? { ...step, stepId: step.id }
    : {
        playbookId: pb.id, stepId: null, name: '', code: '', actorRoleId: null,
        promptTemplate: '', requireVerdict: false, onRejectOrder: null
      })
  stepForm.playbookId = pb.id
  stepDialog.value = true
}

async function saveStep() {
  const data = {
    name: stepForm.name, code: stepForm.code, actorRoleId: stepForm.actorRoleId,
    promptTemplate: stepForm.promptTemplate, requireVerdict: stepForm.requireVerdict,
    onRejectOrder: stepForm.requireVerdict ? stepForm.onRejectOrder : null
  }
  try {
    if (stepForm.stepId) await api.updateStep(stepForm.stepId, data)
    else await api.addStep(stepForm.playbookId, data)
    ElMessage.success('已保存')
    stepDialog.value = false
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function move(step, dir) {
  try {
    await api.moveStep(step.id, dir)
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function del(pb, step) {
  try {
    await api.deleteStep(step.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>
