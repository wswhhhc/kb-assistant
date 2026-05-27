<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background: #304156">
      <div class="logo-area">
        <h2 style="color: #fff; text-align: center; padding: 16px 0; margin: 0; font-size: 16px;">知识库智能助手</h2>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>首页仪表盘</span>
        </el-menu-item>
        <el-menu-item v-if="authStore.isAdmin" index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/knowledge-bases">
          <el-icon><Folder /></el-icon>
          <span>知识库管理</span>
        </el-menu-item>
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>智能问答</span>
        </el-menu-item>
        <el-menu-item v-if="authStore.isAdmin" index="/feedback">
          <el-icon><Star /></el-icon>
          <span>反馈记录</span>
        </el-menu-item>
        <el-menu-item v-if="authStore.isAdmin" index="/failed-questions">
          <el-icon><Warning /></el-icon>
          <span>失败问题</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: flex-end; padding: 0 20px;">
        <span style="margin-right: 16px; font-size: 14px;">{{ authStore.userInfo?.realName || authStore.userInfo?.username }}</span>
        <el-dropdown @command="handleCommand">
          <span class="el-dropdown-link" style="cursor: pointer;">
            {{ authStore.isAdmin ? '管理员' : '用户' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main style="background: #f0f2f5; padding: 20px;">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import {
  DataAnalysis, User, Folder, ChatDotRound, Star, Warning, ArrowDown
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

function handleCommand(command) {
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.el-aside {
  overflow-y: auto;
}
.el-menu {
  border-right: none;
}
.logo-area {
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
</style>
