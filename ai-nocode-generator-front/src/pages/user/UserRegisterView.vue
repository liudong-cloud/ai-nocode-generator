<template>
  <div class="register-page">
    <div class="register-card">
      <div class="register-header">
        <img class="register-logo" src="@/assets/logo.png" alt="logo" />
        <h2 class="register-title">注册账号</h2>
        <p class="register-subtitle">加入 AI 应用生成平台</p>
      </div>
      <a-form
        :model="formState"
        layout="vertical"
        @finish="handleRegister"
        class="register-form"
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
        <a-form-item
          label="确认密码"
          name="checkPassword"
          :rules="[{ required: true, message: '请确认密码' }]"
        >
          <a-input-password
            v-model:value="formState.checkPassword"
            placeholder="请再次输入密码"
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
            class="register-btn"
          >
            注册
          </a-button>
        </a-form-item>
        <div class="register-footer">
          已有账号？
          <router-link to="/user/login">立即登录</router-link>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { userRegister } from '@/api/userController'

const router = useRouter()
const loading = ref(false)

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const handleRegister = async () => {
  if (formState.userPassword !== formState.checkPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const res = await userRegister(formState)
    if (res.data.code === 0) {
      message.success('注册成功，请登录')
      router.push('/user/login')
    } else {
      message.error(res.data.message || '注册失败')
    }
  } catch (e: any) {
    message.error('注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0f7f5 0%, #f0f4ff 50%, #fce4ec 100%);
}

.register-card {
  width: 420px;
  padding: 48px 40px 32px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.register-header {
  text-align: center;
  margin-bottom: 36px;
}

.register-logo {
  height: 56px;
  margin-bottom: 12px;
}

.register-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px;
}

.register-subtitle {
  color: #888;
  font-size: 14px;
  margin: 0;
}

.register-form :deep(.ant-input-affix-wrapper) {
  border-radius: 10px;
}

.register-btn {
  border-radius: 10px;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #38b2ac, #4299e1);
  border: none;
}

.register-btn:hover {
  background: linear-gradient(135deg, #2c9e97, #3182ce);
}

.register-footer {
  text-align: center;
  color: #888;
  font-size: 14px;
}

.register-footer a {
  color: #38b2ac;
  font-weight: 500;
}
</style>
