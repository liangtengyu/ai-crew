<template>
  <div v-if="detail.task">
    <div class="page-card" style="display: flex; align-items: center; gap: 14px; flex-wrap: wrap">
      <el-button @click="$router.push('/tasks')">← 返回</el-button>
      <b style="font-size: 16px">{{ detail.task.title }}</b>
      <status-tag :status="detail.task.status" />
      <span style="color: #909399; font-size: 13px">
        剧本：{{ detail.playbookName }} · 第 {{ detail.task.round }}/{{ detail.maxRounds }} 轮
      </span>
      <span style="flex: 1"></span>
      <el-button v-if="detail.task.status === 'FAILED' || detail.task.status === 'CREATED'"
        type="warning" @click="launch">▶ 继续 / 启动协作</el-button>
      <template v-if="detail.task.status === 'DELIVERED'">
        <el-button type="success" @click="accept">✅ 采纳交付</el-button>
        <el-button type="danger" @click="reject">↩ 退回重做</el-button>
      </template>
    </div>

    <el-row :gutter="16">
      <el-col :span="17">
        <div class="chat-box" ref="chatBox">
          <div v-for="m in detail.messages" :key="m.id" class="msg"
            :class="{ right: m.senderType === 'BOSS', 'system-msg': m.senderType === 'SYSTEM' }">
            <template v-if="m.senderType === 'SYSTEM'">
              <div class="sys-text">{{ m.content }}</div>
            </template>
            <template v-else>
              <div class="avatar" :style="{ background: m.senderType === 'BOSS' ? '#409eff' : (colorOf(m) || '#909399') }">
                {{ m.emoji || '🤖' }}
              </div>
              <div class="body">
                <div class="meta">
                  <b :style="{ color: m.senderType === 'BOSS' ? '#409eff' : '#303133' }">{{ m.senderName }}</b>
                  <span v-if="m.targetName">→ {{ m.targetName }}</span>
                  <el-tag v-if="m.verdict === 'APPROVE'" type="success" size="small">验收通过</el-tag>
                  <el-tag v-else-if="m.verdict === 'REJECT'" type="danger" size="small">打回返工</el-tag>
                  <span>{{ fmtTime(m.createdAt) }}</span>
                </div>
                <div class="bubble"
                  :class="{ delivery: m.msgType === 'DELIVERY', 'error-bubble': m.msgType === 'ERROR' }"
                  v-html="renderMd(m.content)"></div>
              </div>
            </template>
          </div>
        </div>
      </el-col>

      <el-col :span="7">
        <div class="page-card">
          <div class="toolbar"><h3>流程进度</h3></div>
          <div v-for="s in detail.steps" :key="s.stepOrder" class="step-item" :class="s.state">
            <span>{{ iconOf(s) }}</span>
            <span>{{ s.stepOrder }}. {{ s.name }}</span>
            <span style="flex: 1"></span>
            <span style="font-size: 12px">{{ s.emoji }} {{ s.actorName }}</span>
            <el-tag v-if="s.requireVerdict" size="small" type="warning">关卡</el-tag>
          </div>
        </div>

        <div class="page-card" v-if="task.result">
          <div class="toolbar"><h3>📦 交付物</h3></div>
          <div style="max-height: 320px; overflow: auto" v-html="renderMd(task.result)"></div>
        </div>

        <el-collapse class="page-card" style="padding: 0 12px">
          <el-collapse-item title="🧑‍💼 需求原文" :name="'req'">
            <div style="white-space: pre-wrap; font-size: 13px; line-height: 1.7">{{ detail.task.requirement }}</div>
          </el-collapse-item>
        </el-collapse>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { api } from '../api'
import { fmtTime, renderMd } from '../utils/md'
import StatusTag from '../components/StatusTag.vue'

const route = useRoute()
const id = route.params.id
const detail = reactive({ task: null, messages: [], steps: [], playbookName: '', maxRounds: 3 })
const chatBox = ref(null)
let es = null

const task = computed(() => detail.task || {})

function colorOf(m) {
  const step = detail.steps.find((s) => s.stepOrder === m.stepOrder)
  if (step) return step.color
  return null
}

function iconOf(s) {
  return s.state === 'done' ? '✅' : s.state === 'active' ? '🔄' : '⏳'
}

function scrollBottom() {
  nextTick(() => {
    if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
  })
}

async function load() {
  try {
    const d = await api.taskDetail(id)
    Object.assign(detail, d)
    scrollBottom()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function connect() {
  es = new EventSource(`/api/tasks/${id}/stream`)
  es.addEventListener('message', (e) => {
    detail.messages.push(JSON.parse(e.data))
    scrollBottom()
  })
  es.addEventListener('status', () => load())
  es.onerror = () => {
    es.close()
    setTimeout(() => {
      if (!es || es.readyState === EventSource.CLOSED) connect()
    }, 4000)
  }
}

async function launch() {
  try {
    await api.launchTask(id)
    ElMessage.success('协作已启动')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function accept() {
  try {
    const { value } = await ElMessageBox.prompt('可选填一句验收意见', 'BOSS · 采纳交付', {
      confirmButtonText: '采纳', cancelButtonText: '取消', inputPlaceholder: '例如：干得漂亮'
    })
    await api.acceptTask(id, value || '')
    ElMessage.success('已采纳 🎉')
    load()
  } catch (e) {
    if (e !== 'cancel' && e?.message) ElMessage.error(e.message)
  }
}

async function reject() {
  try {
    const { value } = await ElMessageBox.prompt('请填写退回意见（会同步给团队重新协作）', 'BOSS · 退回重做', {
      confirmButtonText: '退回', cancelButtonText: '取消',
      inputPlaceholder: '例如：方案成本太高，改用轻量实现；登录要支持手机号',
      inputValidator: (v) => (v && v.trim() ? true : '退回意见不能为空')
    })
    await api.rejectTask(id, value)
    ElMessage.success('已退回，团队重新协作中')
    load()
  } catch (e) {
    if (e !== 'cancel' && e?.message) ElMessage.error(e.message)
  }
}

onMounted(() => {
  load()
  connect()
})

onBeforeUnmount(() => {
  if (es) es.close()
})
</script>
