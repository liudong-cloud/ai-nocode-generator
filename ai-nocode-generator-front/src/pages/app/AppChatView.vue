<template>
  <div class="chat-page">
    <!-- 顶部栏 -->
    <div class="chat-topbar">
      <div class="topbar-left">
        <a-button type="text" @click="router.push('/')">
          <template #icon><ArrowLeftOutlined /></template>
        </a-button>
        <span class="app-title">{{ appInfo?.appName || '应用生成' }}</span>
      </div>
      <div class="topbar-right">
        <a-button
          type="default"
          class="edit-btn"
          style="margin-right: 8px"
          @click="router.push(`/app/edit/${appId}`)"
        >
          <template #icon><EditOutlined /></template>
          编辑
        </a-button>
        <a-button
          v-if="deployUrl"
          type="link"
          :href="deployUrl"
          target="_blank"
        >
          <template #icon><LinkOutlined /></template>
          访问链接
        </a-button>
        <a-button
          type="primary"
          :loading="deploying"
          class="deploy-btn"
          @click="handleDeploy"
        >
          <template #icon><CloudUploadOutlined /></template>
          部署
        </a-button>
      </div>
    </div>

    <!-- 核心内容区域 -->
    <div class="chat-body">
      <!-- 左侧对话区域 -->
      <div class="chat-left">
        <div class="messages-area" ref="messagesRef">
          <div
            v-for="(msg, index) in messages"
            :key="index"
            :class="['message-item', msg.role === 'user' ? 'message-user' : 'message-ai']"
          >
            <div class="message-avatar">
              <a-avatar v-if="msg.role === 'user'" :size="32" style="background-color: #38b2ac">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <a-avatar v-else :size="32" style="background-color: #f59e0b">
                <template #icon><RobotOutlined /></template>
              </a-avatar>
            </div>
            <div class="message-content">
              <div class="message-bubble" v-html="renderMarkdown(msg.content)"></div>
            </div>
          </div>
          <!-- AI 正在生成提示 -->
          <div v-if="isGenerating" class="message-item message-ai">
            <div class="message-avatar">
              <a-avatar :size="32" style="background-color: #f59e0b">
                <template #icon><RobotOutlined /></template>
              </a-avatar>
            </div>
            <div class="message-content">
              <div class="message-bubble typing-indicator">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input-area">
          <div class="chat-input-wrapper">
            <a-textarea
              v-model:value="userInput"
              placeholder="输入消息继续对话..."
              :auto-size="{ minRows: 1, maxRows: 4 }"
              class="chat-textarea"
              :disabled="isStreaming"
              @pressEnter.prevent="handleSendMessage"
            />
            <a-button
              type="primary"
              shape="circle"
              class="chat-send-btn"
              :disabled="isStreaming || !userInput.trim()"
              @click="handleSendMessage"
            >
              <template #icon><SendOutlined /></template>
            </a-button>
          </div>
        </div>
      </div>

      <!-- 右侧预览区域 -->
      <div class="chat-right">
        <div class="preview-header">
          <span class="preview-title">生成效果预览</span>
          <a-button
            v-if="previewUrl"
            type="link"
            size="small"
            :href="previewUrl"
            target="_blank"
          >
            <template #icon><ExportOutlined /></template>
            新窗口打开
          </a-button>
        </div>
        <div class="preview-body">
          <iframe
            v-if="previewUrl"
            :src="previewUrl"
            class="preview-iframe"
            sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
          ></iframe>
          <div v-else class="preview-empty">
            <div class="preview-empty-icon">
              <DesktopOutlined />
            </div>
            <p>网站生成后将在此展示效果</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  SendOutlined,
  CloudUploadOutlined,
  LinkOutlined,
  UserOutlined,
  RobotOutlined,
  DesktopOutlined,
  ExportOutlined,
  EditOutlined,
} from '@ant-design/icons-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css' // Github style for code block
import { getAppVoById, deploy } from '@/api/appController'
import CONFIG from '@/config'

const router = useRouter()
const route = useRoute()

const appId = route.params.id as string
const appInfo = ref<API.AppVO>()
const deploying = ref(false)
const deployUrl = ref('')
const previewUrl = ref('')
const userInput = ref('')
const isStreaming = ref(false)
const isGenerating = ref(false)
const messagesRef = ref<HTMLElement>()

interface ChatMessage {
  role: 'user' | 'ai'
  content: string
}

const messages = ref<ChatMessage[]>([])

// 加载应用信息
const loadAppInfo = async () => {
  try {
    const res = await getAppVoById({ id: appId })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      // 如果有 deployKey，构建预览 URL
      if (res.data.data.codeGenType && res.data.data.id) {
        previewUrl.value = `${CONFIG.baseURL}/static/${res.data.data.codeGenType}_${res.data.data.id}/`
      }
    }
  } catch (e) {
    message.error('加载应用信息失败')
  }
}

// Markdown 渲染配置
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
               hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
               '</code></pre>';
      } catch (__) {}
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>';
  }
});

const renderMarkdown = (text: string) => {
  if (!text) return ''
  return md.render(text)
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

watch(messages, () => {
  scrollToBottom()
}, { deep: true })

// SSE 对话
const sendChatMessage = async (prompt: string) => {
  isStreaming.value = true
  isGenerating.value = true
  // 先清除之前的预览
  previewUrl.value = ''

  // 添加AI占位消息
  const aiMsgIndex = messages.value.length
  messages.value.push({ role: 'ai', content: '' })
  isGenerating.value = false

  try {
    const url = `${CONFIG.baseURL}/app/chat/gen/code?appId=${appId}&prompt=${encodeURIComponent(prompt)}`
    const eventSource = new EventSource(url, { withCredentials: true })

    eventSource.onmessage = (event) => {
      const data = event.data
      if (data) {
        try {
          // 解析后端返回的 JSON 字符串，取其 d 字段的值
          const jsonData = JSON.parse(data)
          messages.value[aiMsgIndex].content += jsonData.d || ''
          scrollToBottom()
        } catch (e) {
          // 如果解析失败（例如心跳包或格式不完整），则不做处理或记录错误
          console.error('解析 SSE 数据逻辑错误:', e, data)
        }
      }
    }

    eventSource.onerror = async () => {
      eventSource.close()
      isStreaming.value = false
      // 重新加载应用信息（获取最新的 codeGenType 等信息）
      await loadAppInfo()
      // 生成完成后展示预览
      if (appInfo.value?.codeGenType && appInfo.value?.id) {
        previewUrl.value = `${CONFIG.baseURL}/static/${appInfo.value.codeGenType}_${appInfo.value.id}/`
      }
    }
  } catch (e) {
    isStreaming.value = false
    message.error('对话失败，请稍后重试')
  }
}

// 发送消息
const handleSendMessage = async () => {
  const text = userInput.value.trim()
  if (!text || isStreaming.value) return

  messages.value.push({ role: 'user', content: text })
  userInput.value = ''
  scrollToBottom()

  await sendChatMessage(text)
}

// 部署
const handleDeploy = async () => {
  deploying.value = true
  try {
    const res = await deploy({ appId })
    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      message.success('部署成功！')
    } else {
      message.error(res.data.message || '部署失败')
    }
  } catch (e) {
    message.error('部署失败，请稍后重试')
  } finally {
    deploying.value = false
  }
}

onMounted(async () => {
  await loadAppInfo()
  // 判断是新建（带有 query param prompt）还是查看已有应用
  const initialPrompt = route.query.prompt as string
  
  // 只有当 messages 为空，且应用尚未生成过代码 (codeGenType 为空) 时，才自动触发生成
  if (initialPrompt && messages.value.length === 0 && !appInfo.value?.codeGenType) {
    // 新建：自动发送提示词
    messages.value.push({ role: 'user', content: initialPrompt })
    scrollToBottom()
    await sendChatMessage(initialPrompt)
    
    // 生成开始后，尝试清理掉 URL 中的 prompt 参数，防止刷新重复触发
    router.replace({ query: { ...route.query, prompt: undefined } })
  } else if (appInfo.value?.initPrompt && messages.value.length === 0) {
    // 查看已有应用：仅展示对话历史（首条提示词及对应的AI结果预填充）
    messages.value.push({ role: 'user', content: appInfo.value.initPrompt })
    if (appInfo.value.codeGenType && appInfo.value.id) {
      messages.value.push({ role: 'ai', content: '**网站代码已生成**，请在右侧页面查看实际效果。' })
    }
    scrollToBottom()
  }
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 60px);
  margin: -20px;
}

/* 顶部栏 */
.chat-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 20px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  min-height: 48px;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.deploy-btn {
  border-radius: 8px;
  background: linear-gradient(135deg, #f59e0b, #ef6c00);
  border: none;
  font-weight: 600;
}

.deploy-btn:hover {
  background: linear-gradient(135deg, #e08e0a, #d45a00);
}

/* 核心内容区域 */
.chat-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  border-right: 1px solid #f0f0f0;
  background: #fafafa;
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message-user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 80%;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.message-user .message-bubble {
  background: linear-gradient(135deg, #38b2ac, #4299e1);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-ai .message-bubble {
  background: #fff;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.message-bubble :deep(code) {
  padding: 2px 6px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 4px;
  font-size: 13px;
}

.message-bubble :deep(strong) {
  font-weight: 700;
}

/* 打字动画 */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 16px 20px;
}

.typing-indicator .dot {
  width: 8px;
  height: 8px;
  background: #ccc;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator .dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 输入区域 */
.chat-input-area {
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.chat-input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: #f5f5f5;
  border-radius: 16px;
  padding: 8px 12px;
}

.chat-textarea {
  border: none !important;
  box-shadow: none !important;
  background: transparent !important;
  font-size: 14px;
  resize: none;
}

.chat-textarea:focus {
  box-shadow: none !important;
}

.chat-send-btn {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #38b2ac, #4299e1);
  border: none;
}

.chat-send-btn:hover {
  background: linear-gradient(135deg, #2c9e97, #3182ce);
}

/* 右侧预览区域 */
.chat-right {
  width: 50%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #666;
}

.preview-body {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.preview-empty {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #bbb;
}

.preview-empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.3;
}

.preview-empty p {
  font-size: 14px;
}
</style>
