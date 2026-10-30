import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 60000 })

http.interceptors.response.use(
  (r) => {
    const body = r.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 0) return Promise.reject(new Error(body.msg || '请求失败'))
      return body.data
    }
    return body
  },
  (err) => Promise.reject(new Error(err.response?.data?.msg || err.message || '网络异常'))
)

export const api = {
  // 统计
  stats: () => http.get('/stats'),
  // 任务
  listTasks: () => http.get('/tasks'),
  createTask: (data) => http.post('/tasks', data),
  taskDetail: (id) => http.get(`/tasks/${id}`),
  launchTask: (id) => http.post(`/tasks/${id}/launch`),
  acceptTask: (id, comment) => http.post(`/tasks/${id}/accept`, { comment }),
  rejectTask: (id, comment) => http.post(`/tasks/${id}/reject`, { comment }),
  deleteTask: (id) => http.delete(`/tasks/${id}`),
  // 模型
  listModels: () => http.get('/models'),
  createModel: (data) => http.post('/models', data),
  updateModel: (id, data) => http.put(`/models/${id}`, data),
  deleteModel: (id) => http.delete(`/models/${id}`),
  // 角色
  listRoles: () => http.get('/roles'),
  createRole: (data) => http.post('/roles', data),
  updateRole: (id, data) => http.put(`/roles/${id}`, data),
  deleteRole: (id) => http.delete(`/roles/${id}`),
  // 剧本
  listPlaybooks: () => http.get('/playbooks'),
  updatePlaybook: (id, data) => http.put(`/playbooks/${id}`, data),
  addStep: (pbId, data) => http.post(`/playbooks/${pbId}/steps`, data),
  updateStep: (stepId, data) => http.put(`/playbooks/steps/${stepId}`, data),
  deleteStep: (stepId) => http.delete(`/playbooks/steps/${stepId}`),
  moveStep: (stepId, dir) => http.post(`/playbooks/steps/${stepId}/move?dir=${dir}`)
}
