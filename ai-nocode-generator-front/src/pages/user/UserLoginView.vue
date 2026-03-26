<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <img class="login-logo" src="@/assets/logo.png" alt="logo" />
        <h2 class="login-title">AI 应用生成平台</h2>
        <p class="login-subtitle">与 AI 对话轻松创建应用和网站</p>
      </div>
      <a-form
        :model="formState"
        layout="vertical"
        @finish="handleLogin"
        class="login-form"
      >
        <a-form-item
          label="账号"
          name="userAccount"
          :rules="[{ required: true, message: '请输入账号' }]"
        >
          <a-input
            v-model:value="formState.userAccount"
            placeholder="请输入账号"
            size="large"
          >
            <template #prefix>
              <UserOutlined />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item
          label="密码"
          name="userPassword"
          :rules="[{ required: true, message: '请输入密码' }]"
        >
          <a-input-password
            v-model:value="formState.userPassword"
            placeholder="请输入密码"
            size="large"
          >
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>
        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            block
            size="large"
            :loading="loading"
            class="login-btn"
          >
            登录
          </a-button>
        </a-form-item>
        <div class="login-footer">
          还没有账号？
          <router-link to="/user/register">立即注册</router-link>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { userLogin } from '@/api/userController'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const handleLogin = async () => {
  loading.value = true
  try {
    const res = await userLogin(formState)
    if (res.data.code === 0 && res.data.data) {
      message.success('登录成功')
      userStore.setLoginUser(res.data.data)
      // 跳转到 redirect 页面或主页
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    } else {
      message.error(res.data.message || '登录失败')
    }
  } catch (e: any) {
    message.error('登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0f7f5 0%, #f0f4ff 50%, #fce4ec 100%);
}

.login-card {
  width: 420px;
  padding: 48px 40px 32px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-logo {
  height: 56px;
  margin-bottom: 12px;
}

.login-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px;
}

.login-subtitle {
  color: #888;
  font-size: 14px;
  margin: 0;
}

.login-form :deep(.ant-input-affix-wrapper) {
  border-radius: 10px;
}

.login-btn {
  border-radius: 10px;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #38b2ac, #4299e1);
  border: none;
}

.login-btn:hover {
  background: linear-gradient(135deg, #2c9e97, #3182ce);
}

.login-footer {
  text-align: center;
  color: #888;
  font-size: 14px;
}

.login-footer a {
  color: #38b2ac;
  font-weight: 500;
}
</style>
