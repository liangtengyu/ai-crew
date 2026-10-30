import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '概览' } },
  { path: '/tasks', name: 'tasks', component: () => import('../views/Tasks.vue'), meta: { title: '需求大厅' } },
  { path: '/tasks/:id', name: 'taskDetail', component: () => import('../views/TaskDetail.vue'), meta: { title: '作战室' } },
  { path: '/models', name: 'models', component: () => import('../views/Models.vue'), meta: { title: '模型配置' } },
  { path: '/roles', name: 'roles', component: () => import('../views/Roles.vue'), meta: { title: '角色管理' } },
  { path: '/playbooks', name: 'playbooks', component: () => import('../views/Playbooks.vue'), meta: { title: '协作剧本' } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
