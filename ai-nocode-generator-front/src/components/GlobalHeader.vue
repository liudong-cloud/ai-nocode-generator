<template>
  <div id="globalHeader">
    <a-row :wrap="false" align="middle">
      <a-col flex="220px">
        <div class="title-bar" @click="router.push('/')" style="cursor: pointer">
          <img class="logo" src="../assets/logo.png" alt="logo" />
          <div class="title">AI应用生成平台</div>
        </div>
      </a-col>
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="current"
          mode="horizontal"
          :items="menuItems"
          @click="doMenuClick"
        />
      </a-col>
      <a-col flex="180px">
        <div class="user-login-status">
          <template v-if="userStore.loginUser.id">
            <a-dropdown>
              <a class="user-info" @click.prevent>
                <a-avatar
                  v-if="userStore.loginUser.userAvatar"
                  :src="userStore.loginUser.userAvatar"
                  :size="28"
                />
                <a-avatar v-else :size="28" style="background-color: #38b2ac">
                  <template #icon><UserOutlined /></template>
                </a-avatar>
                <span class="user-name">{{ userStore.loginUser.userName || userStore.loginUser.userAccount }}</span>
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="/user/profile" @click="router.push('/user/profile')">
                    <UserOutlined />
                    <span>个人信息</span>
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="logout" @click="handleLogout">
                    <LogoutOutlined />
                    <span>退出登录</span>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
          <template v-else>
            <a-button type="primary" href="/user/login" class="login-btn">登录</a-button>
          </template>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watchEffect } from 'vue'
import type { MenuProps } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LogoutOutlined } from '@ant-design/icons-vue'
import { userLogout } from '@/api/userController'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isAdmin = computed(() => userStore.loginUser.userRole === 'admin')

const menuItems = computed<MenuProps['items']>(() => {
  const items: MenuProps['items'] = [
    {
      key: '/',
      label: '主页',
      title: '主页',
    },
  ]

  if (isAdmin.value) {
    items!.push({
      key: '/admin/app',
      label: '应用管理',
      title: '应用管理',
    })
  }

  return items
})

const current = ref<string[]>(['/'])

watchEffect(() => {
  const path = route.path
  if (path.startsWith('/admin/app')) {
    current.value = ['/admin/app']
  } else if (path === '/') {
    current.value = ['/']
  } else {
    current.value = []
  }
})

// 路由跳转
const doMenuClick = ({ key }: { key: string }) => {
  router.push({
    path: key,
  })
}

// 退出登录
const handleLogout = async () => {
  try {
    const res = await userLogout()
    if (res.data.code === 0) {
      message.success('已退出登录')
      userStore.setLoginUser({})
      router.push('/')
    }
  } catch (e) {
    message.error('退出登录失败')
  }
}
</script>

<style scoped>
.title-bar {
  display: flex;
  align-items: center;
}

.title {
  color: black;
  font-size: 18px;
  margin-left: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.logo {
  height: 48px;
}

.user-login-status {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #333;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f5f5;
}

.user-name {
  font-size: 14px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.login-btn {
  border-radius: 8px;
  background: linear-gradient(135deg, #38b2ac, #4299e1);
  border: none;
}
</style>
