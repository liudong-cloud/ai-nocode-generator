<template>
  <div id="userProfileView">
    <a-card class="profile-card" :bordered="false">
      <template #title>
        <div class="card-title">
          <UserOutlined class="title-icon" />
          <span>个人中心</span>
        </div>
      </template>
      <div class="profile-header">
        <div class="avatar-container">
          <a-avatar
            :src="formState.userAvatar"
            :size="100"
            class="main-avatar"
          >
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <div class="avatar-mask" @click="showAvatarModal = true">
            <CameraOutlined />
            <span>更换头像</span>
          </div>
        </div>
        <div class="user-info-brief">
          <h3>{{ userStore.loginUser.userName || '未设置用户名' }}</h3>
          <p class="account-tag">UID: {{ userStore.loginUser.id }}</p>
          <a-tag color="cyan">{{ userStore.loginUser.userRole === 'admin' ? '系统管理员' : '普通用户' }}</a-tag>
        </div>
      </div>

      <a-divider />

      <a-form
        :model="formState"
        layout="vertical"
        @finish="handleSubmit"
        class="profile-form"
      >
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="用户账号">
              <a-input v-model:value="userStore.loginUser.userAccount" disabled class="fancy-input" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="用户名"
              name="userName"
              :rules="[{ required: true, message: '请输入用户名' }]"
            >
              <a-input v-model:value="formState.userName" placeholder="给自己起个好听的名字吧" class="fancy-input" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="用户头像 URL" name="userAvatar">
           <a-input v-model:value="formState.userAvatar" placeholder="输入头像图片 URL" class="fancy-input">
             <template #prefix><LinkOutlined /></template>
           </a-input>
        </a-form-item>

        <a-form-item label="个人简介" name="userProfile">
          <a-textarea
            v-model:value="formState.userProfile"
            placeholder="像世人介绍一下你自己..."
            :rows="4"
            class="fancy-textarea"
          />
        </a-form-item>

        <a-form-item class="form-actions">
          <a-button type="primary" html-type="submit" :loading="loading" class="save-btn" block>
            <SaveOutlined /> 保存个人信息
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useUserStore } from '@/stores/user';
import { updateUser } from '@/api/userController';
import { message } from 'ant-design-vue';
import { 
    UserOutlined, 
    CameraOutlined, 
    LinkOutlined, 
    SaveOutlined 
} from '@ant-design/icons-vue';

const userStore = useUserStore();
const loading = ref(false);
const showAvatarModal = ref(false);

const formState = reactive<API.UserUpdateRequest>({
  userName: '',
  userAvatar: '',
  userProfile: '',
});

onMounted(async () => {
    if (!userStore.loginUser.id) {
        await userStore.fetchLoginUser();
    }
    const { userName, userAvatar, userProfile } = userStore.loginUser;
    formState.userName = userName;
    formState.userAvatar = userAvatar;
    formState.userProfile = userProfile;
});

const handleSubmit = async () => {
    loading.value = true;
    try {
        const res = await updateUser({
            ...formState,
            id: userStore.loginUser.id,
        });
        if (res.data.code === 0) {
            message.success('更新成功');
            await userStore.fetchLoginUser();
        } else {
            message.error('更新失败: ' + res.data.message);
        }
    } catch (e: any) {
        message.error('请求出错: ' + (e.message || '未知错误'));
    } finally {
        loading.value = false;
    }
}
</script>

<style scoped>
#userProfileView {
  max-width: 800px;
  margin: 40px auto;
  padding: 0 20px;
}

.profile-card {
  border-radius: 24px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.05);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  overflow: hidden;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
}

.title-icon {
  color: #38b2ac;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 20px 0;
}

.avatar-container {
  position: relative;
  cursor: pointer;
}

.main-avatar {
  border: 4px solid #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  background: #38b2ac;
}

.avatar-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: white;
  opacity: 0;
  transition: opacity 0.3s;
  font-size: 12px;
  gap: 4px;
}

.avatar-container:hover .avatar-mask {
  opacity: 1;
}

.user-info-brief h3 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
}

.account-tag {
  color: #888;
  margin-bottom: 8px;
  font-family: monospace;
}

.fancy-input :deep(.ant-input),
.fancy-textarea :deep(.ant-input) {
  border-radius: 12px;
  padding: 10px 16px;
  background-color: #fcfdfe;
  border: 1px solid #eef2f6;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fancy-input :deep(.ant-input):focus,
.fancy-textarea :deep(.ant-input):focus {
  background-color: #fff;
  border-color: #38b2ac;
  box-shadow: 0 0 0 4px rgba(56, 178, 172, 0.1);
}

.save-btn {
  height: 50px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #38b2ac, #4299e1);
  border: none;
  box-shadow: 0 10px 20px rgba(56, 178, 172, 0.2);
  transition: all 0.3s;
}

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(56, 178, 172, 0.3);
  background: linear-gradient(135deg, #2c9e97, #3182ce);
}

.form-actions {
  margin-top: 20px;
}

:deep(.ant-form-item-label label) {
    font-weight: 600;
    color: #4a5568;
}

@media (max-width: 640px) {
  .profile-header {
    flex-direction: column;
    text-align: center;
  }
}
</style>
