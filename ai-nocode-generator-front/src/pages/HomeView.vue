<template>
  <div class="home-page">
    <!-- Hero 区域 -->
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">
          一句话
          <img class="hero-icon" src="@/assets/logo.png" alt="logo" />
          呈所想
        </h1>
        <p class="hero-subtitle">与 AI 对话轻松创建应用和网站</p>
        <!-- 输入框 -->
        <div class="prompt-input-wrapper">
          <a-textarea
            v-model:value="promptText"
            placeholder="使用 NoCode 创建一个高效的小工具，帮我计算......"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            class="prompt-textarea"
            @pressEnter="handleCreateApp"
          />
          <div class="prompt-actions">
            <a-button
              type="primary"
              shape="circle"
              :loading="creating"
              class="send-btn"
              @click="handleCreateApp"
            >
              <template #icon>
                <SendOutlined />
              </template>
            </a-button>
          </div>
        </div>
        <!-- 快捷标签 -->
        <div class="quick-tags">
          <a-tag
            v-for="tag in quickTags"
            :key="tag"
            class="quick-tag"
            @click="handleQuickTag(tag)"
          >
            {{ tag }}
          </a-tag>
        </div>
      </div>
    </section>

    <!-- 我的应用 -->
    <section class="app-section" v-if="userStore.loginUser.id">
      <div class="section-header">
        <h2 class="section-title">我的应用</h2>
        <a-input-search
          v-model:value="mySearchName"
          placeholder="搜索应用名称"
          style="width: 240px"
          allow-clear
          @search="fetchMyApps"
        />
      </div>
      <a-spin :spinning="myLoading">
        <div class="app-grid" v-if="myApps.length > 0">
          <div
            class="app-card"
            v-for="app in myApps"
            :key="app.id"
          >
            <div class="app-card-cover">
              <img
                v-if="app.cover"
                :src="app.cover"
                :alt="app.appName"
                class="cover-img"
              />
              <div v-else class="cover-placeholder">
                <AppstoreOutlined class="cover-icon" />
              </div>
              <div class="card-actions">
                <a-button type="primary" size="small" @click.stop="goToChat(app, true)">
                  查看
                </a-button>
                <a-button size="small" @click.stop="goToEdit(app)">
                  编辑
                </a-button>
              </div>
            </div>
            <div class="app-card-info">
              <div class="app-name">{{ app.appName || '未命名应用' }}</div>
              <div class="app-meta">
                <span>{{ formatDate(app.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无应用，输入提示词开始创建吧" />
      </a-spin>
      <div class="pagination-wrapper" v-if="myTotal > 0">
        <a-pagination
          v-model:current="myPage"
          :total="myTotal"
          :page-size="myPageSize"
          show-size-changer
          :show-total="(total: number) => `共 ${total} 个应用`"
          @change="fetchMyApps"
        />
      </div>
    </section>

    <!-- 精选应用 -->
    <section class="app-section">
      <div class="section-header">
        <h2 class="section-title">🌟 精选应用</h2>
        <a-input-search
          v-model:value="featuredSearchName"
          placeholder="搜索应用名称"
          style="width: 240px"
          allow-clear
          @search="fetchFeaturedApps"
        />
      </div>
      <a-spin :spinning="featuredLoading">
        <div class="app-grid" v-if="featuredApps.length > 0">
          <div
            class="app-card"
            v-for="app in featuredApps"
            :key="app.id"
          >
            <div class="app-card-cover">
              <img
                v-if="app.cover"
                :src="app.cover"
                :alt="app.appName"
                class="cover-img"
              />
              <div v-else class="cover-placeholder">
                <AppstoreOutlined class="cover-icon" />
              </div>
              <div class="card-actions">
                <a-button type="primary" size="small" @click.stop="goToChat(app, true)">
                  查看
                </a-button>
                <a-button size="small" @click.stop="goToEdit(app)">
                  编辑
                </a-button>
              </div>
            </div>
            <div class="app-card-info">
              <div class="app-name">{{ app.appName || '未命名应用' }}</div>
              <div class="app-meta">
                <a-avatar
                  v-if="app.user?.userAvatar"
                  :src="app.user.userAvatar"
                  :size="20"
                />
                <span>{{ app.user?.userName || '匿名用户' }}</span>
                <span class="meta-divider">·</span>
                <span>{{ formatDate(app.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无精选应用" />
      </a-spin>
      <div class="pagination-wrapper" v-if="featuredTotal > 0">
        <a-pagination
          v-model:current="featuredPage"
          :total="featuredTotal"
          :page-size="featuredPageSize"
          show-size-changer
          :show-total="(total: number) => `共 ${total} 个应用`"
          @change="fetchFeaturedApps"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SendOutlined, AppstoreOutlined } from '@ant-design/icons-vue'
import { addApp, listMyAppByPage, listFeaturedAppByPage } from '@/api/appController'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// 创建应用
const promptText = ref('')
const creating = ref(false)
const quickTags = ['波普风电商页面', '企业网站', '电商运营后台', '暗黑话题社区']

const handleQuickTag = (tag: string) => {
  promptText.value = `帮我创建一个${tag}`
}

const handleCreateApp = async () => {
  if (!promptText.value.trim()) {
    message.warning('请输入提示词')
    return
  }
  creating.value = true
  try {
    const prompt = promptText.value.trim()
    const res = await addApp({ initPrompt: prompt })
    if (res.data.code === 0 && res.data.data) {
      const appId = res.data.data
      message.success('应用创建成功')
      router.push({ path: `/app/chat/${appId}`, query: { prompt } })
    } else {
      message.error(res.data.message || '创建失败')
    }
  } catch (e) {
    message.error('创建失败，请稍后重试')
  } finally {
    creating.value = false
  }
}

// 我的应用
const myApps = ref<API.AppVO[]>([])
const myPage = ref(1)
const myPageSize = ref(20)
const myTotal = ref(0)
const myLoading = ref(false)
const mySearchName = ref('')

const fetchMyApps = async () => {
  myLoading.value = true
  try {
    const res = await listMyAppByPage({
      pageNum: myPage.value,
      pageSize: myPageSize.value,
      appName: mySearchName.value || undefined,
      sortField: 'createTime',
      sortOrder: 'desc',
    })
    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myTotal.value = Number(res.data.data.totalRow) || 0
    }
  } catch (e) {
    // ignore
  } finally {
    myLoading.value = false
  }
}

// 精选应用
const featuredApps = ref<API.AppVO[]>([])
const featuredPage = ref(1)
const featuredPageSize = ref(20)
const featuredTotal = ref(0)
const featuredLoading = ref(false)
const featuredSearchName = ref('')

const fetchFeaturedApps = async () => {
  featuredLoading.value = true
  try {
    const res = await listFeaturedAppByPage({
      pageNum: featuredPage.value,
      pageSize: featuredPageSize.value,
      appName: featuredSearchName.value || undefined,
      sortField: 'priority',
      sortOrder: 'desc',
    })
    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredTotal.value = Number(res.data.data.totalRow) || 0
    }
  } catch (e) {
    // ignore
  } finally {
    featuredLoading.value = false
  }
}

const goToChat = (app: API.AppVO, viewOnly = false) => {
  if (viewOnly) {
    router.push({ path: `/app/chat/${app.id}`, query: { mode: 'view' } })
    return
  }
  router.push(`/app/chat/${app.id}`)
}

const goToEdit = (app: API.AppVO) => {
  router.push(`/app/edit/${app.id}`)
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  if (userStore.loginUser.id) {
    fetchMyApps()
  }
  fetchFeaturedApps()
})
</script>

<style scoped>
.home-page {
  max-width: 1200px;
  margin: 0 auto;
}

/* Hero 区域 */
.hero-section {
  text-align: center;
  padding: 60px 20px 40px;
  background: linear-gradient(135deg, rgba(224, 247, 245, 0.4) 0%, rgba(240, 244, 255, 0.4) 50%, rgba(252, 228, 236, 0.3) 100%);
  border-radius: 24px;
  margin-bottom: 48px;
}

.hero-content {
  max-width: 700px;
  margin: 0 auto;
}

.hero-title {
  font-size: 42px;
  font-weight: 800;
  color: #1a1a2e;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.hero-icon {
  height: 48px;
  width: 48px;
  object-fit: contain;
}

.hero-subtitle {
  font-size: 16px;
  color: #38b2ac;
  margin-bottom: 32px;
}

.prompt-input-wrapper {
  position: relative;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  padding: 16px 16px 12px;
}

.prompt-textarea {
  border: none !important;
  box-shadow: none !important;
  font-size: 15px;
  resize: none;
  padding-right: 48px;
}

.prompt-textarea:focus {
  box-shadow: none !important;
}

.prompt-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.send-btn {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #38b2ac, #4299e1);
  border: none;
}

.send-btn:hover {
  background: linear-gradient(135deg, #2c9e97, #3182ce);
}

.quick-tags {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.quick-tag {
  cursor: pointer;
  border-radius: 20px;
  padding: 4px 16px;
  font-size: 13px;
  border-color: #d9d9d9;
  color: #555;
  transition: all 0.3s;
}

.quick-tag:hover {
  color: #38b2ac;
  border-color: #38b2ac;
  background: rgba(56, 178, 172, 0.05);
}

/* 应用列表区域 */
.app-section {
  margin-bottom: 48px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

.app-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
}

.app-card-cover {
  position: relative;
  width: 100%;
  height: 160px;
  overflow: hidden;
  background: linear-gradient(135deg, #e8f5f3, #eef2ff);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-icon {
  font-size: 48px;
  color: #bbb;
}

.card-actions {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(0, 0, 0, 0.45);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.app-card:hover .card-actions {
  opacity: 1;
}

.app-card-info {
  padding: 14px 16px;
}

.app-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #999;
}

.meta-divider {
  color: #ddd;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
