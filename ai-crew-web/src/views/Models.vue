<template>
  <div class="page-card">
    <div class="toolbar">
      <h3>🧠 模型配置</h3>
      <el-button type="primary" @click="open()">+ 新增模型</el-button>
    </div>
    <el-alert type="info" :closable="false" style="margin-bottom: 12px"
      title="兼容 OpenAI /chat/completions 协议的大模型均可接入（DeepSeek / GLM / Qwen / OpenAI / 各类中转）。Base URL 需满足 {base}/chat/completions 可访问，例如 DeepSeek 填 https://api.deepseek.com，GLM 填 https://open.bigmodel.cn/api/paas/v4。" />
    <el-table :data="list">
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="baseUrl" label="Base URL" min-width="240" />
      <el-table-column prop="model" label="模型 ID" min-width="160" />
      <el-table-column label="apiKey" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.apiKey" type="success" size="small">已配置</el-tag>
          <el-tag v-else type="danger" size="small">未配置</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="temperature" label="温度" width="70" />
      <el-table-column label="启用" width="80">
        <template #default="{ row }"><el-switch v-model="row.enabled" @change="save(row)" /></template>
      </el-table-column>
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-popconfirm title="删除该模型？" @confirm="del(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑模型' : '新增模型'" width="560px">
      <el-form label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如：DeepSeek 官方" />
        </el-form-item>
        <el-form-item label="Base URL" required>
          <el-input v-model="form.baseUrl" placeholder="如：https://api.deepseek.com" />
        </el-form-item>
        <el-form-item label="模型 ID" required>
          <el-input v-model="form.model" placeholder="如：deepseek-chat / glm-4-flash" />
        </el-form-item>
        <el-form-item label="apiKey">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item label="温度">
          <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" style="width: 90%" />
        </el-form-item>
        <el-form-item label="max tokens">
          <el-input-number v-model="form.maxTokens" :min="256" :max="65536" :step="256" />
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

const list = ref([])
const dialog = ref(false)
const form = reactive({})

async function load() {
  try {
    list.value = await api.listModels()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function open(row) {
  Object.assign(form, row || {
    name: '', baseUrl: '', model: '', apiKey: '', temperature: 0.7, maxTokens: 4096, enabled: true
  })
  dialog.value = true
}

async function save(row) {
  const target = row?.id ? row : form
  try {
    if (target.id) await api.updateModel(target.id, target)
    else await api.createModel(target)
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function del(row) {
  try {
    await api.deleteModel(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>
