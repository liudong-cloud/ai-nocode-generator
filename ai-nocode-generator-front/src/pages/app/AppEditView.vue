<template>
  <div class="app-edit-page">
    <div class="page-header">
      <a-button type="text" @click="router.back()">
        <template #icon><ArrowLeftOutlined /></template>
        返回
      </a-button>
      <h2 class="page-title">编辑应用信息</h2>
    </div>

    <a-spin :spinning="loading">
      <div class="edit-card">
        <a-form
          :model="formState"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 16 }"
          @finish="handleSubmit"
        >
          <a-form-item label="应用ID">
            <span>{{ formState.id }}</span>
          </a-form-item>
          <a-form-item
            label="应用名称"
            name="appName"
            :rules="[{ required: true, message: '请输入应用名称' }]"
          >
            <a-input
              v-model:value="formState.appName"
              placeholder="请输入应用名称"
            />
          </a-form-item>
          <!-- 管理员可以编辑封面和优先级 -->
          <template v-if="isAdmin">
            <a-form-item label="封面URL" name="cover">
              <a-input
                v-model:value="formState.cover"
                placeholder="请输入封面图片URL"
              />
            </a-form-item>
            <a-form-item v-if="formState.cover" label="封面预览">
              <img
                :src="formState.cover"
                alt="封面预览"
                style="max-width: 300px; max-height: 200px; border-radius: 8px"
              />
            </a-form-item>
            <a-form-item label="优先级" name="priority">
              <a-input-number
                v-model:value="formState.priority"
                :min="0"
                :max="999"
                placeholder="请输入优先级"
              />
              <span style="margin-left: 8px; color: #999">优先级 ≥ 99 为精选</span>
            </a-form-item>
          </template>
          <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
            <a-button type="primary" html-type="submit" :loading="submitting" class="submit-btn">
              保存修改
            </a-button>
          </a-form-item>
        </a-form>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { getAppVoById, updateMyApp, updateAppByAdmin } from '@/api/appController'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)

const appId = route.params.id as string
const isAdmin = computed(() => userStore.loginUser.userRole === 'admin')

const formState = reactive<API.AppUpdateRequest>({
  id: appId as any,
  appName: '',
  cover: '',
  priority: 0,
})

const loadAppInfo = async () => {
  loading.value = true
  try {
    const res = await getAppVoById({ id: appId })
    if (res.data.code === 0 && res.data.data) {
      const app = res.data.data
      formState.appName = app.appName || ''
      formState.cover = app.cover || ''
      formState.priority = app.priority || 0
    } else {
      message.error('应用不存在')
      router.back()
    }
  } catch (e) {
    message.error('加载应用信息失败')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const updateFn = isAdmin.value ? updateAppByAdmin : updateMyApp
    const res = await updateFn(formState)
    if (res.data.code === 0) {
      message.success('修改成功')
      router.back()
    } else {
      message.error(res.data.message || '修改失败')
    }
  } catch (e) {
    message.error('修改失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadAppInfo()
})
</script>

<style scoped>
.app-edit-page {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.edit-card {
  background: #fff;
  padding: 40px 32px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.submit-btn {
  border-radius: 8px;
  height: 40px;
  padding: 0 32px;
  font-weight: 600;
  background: linear-gradient(135deg, #38b2ac, #4299e1);
  border: none;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #2c9e97, #3182ce);
}
</style>
