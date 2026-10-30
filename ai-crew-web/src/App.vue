<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="logo">🤖 AI Crew</div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/">📊 概览</el-menu-item>
        <el-menu-item index="/tasks">📋 需求大厅</el-menu-item>
        <el-menu-item index="/models">🧠 模型配置</el-menu-item>
        <el-menu-item index="/roles">👥 角色管理</el-menu-item>
        <el-menu-item index="/playbooks">🎬 协作剧本</el-menu-item>
      </el-menu>
      <div style="padding: 14px 18px; margin-top: auto; font-size: 12px; color: #7f8c9b; line-height: 1.7">
        你是 BOSS，提出需求后<br />由 AI 角色团队协作交付
      </div>
    </el-aside>
    <el-main class="main-area">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
</script>
