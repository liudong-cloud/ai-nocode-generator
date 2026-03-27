<template>
  <div class="admin-chat-page">
    <div class="page-header">
      <h2 class="page-title">对话记录管理</h2>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <a-form layout="inline" :model="searchForm">
        <a-form-item label="应用ID">
          <a-input
            v-model:value="searchForm.appId"
            placeholder="请输入应用ID"
            allow-clear
            style="width: 180px"
          />
        </a-form-item>
        <a-form-item label="用户ID">
          <a-input
            v-model:value="searchForm.userId"
            placeholder="请输入用户ID"
            allow-clear
            style="width: 180px"
          />
        </a-form-item>
        <a-form-item label="消息内容">
          <a-input
            v-model:value="searchForm.message"
            placeholder="搜索消息内容"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="消息类型">
          <a-select
            v-model:value="searchForm.messageType"
            placeholder="全部"
            allow-clear
            style="width: 120px"
          >
            <a-select-option value="user">User</a-select-option>
            <a-select-option value="ai">AI</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            搜索
          </a-button>
          <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>
    </div>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="historyList"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      :scroll="{ x: 1000 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'messageType'">
          <a-tag :color="record.messageType === 'user' ? 'blue' : 'green'">
            {{ record.messageType === 'user' ? '用户' : 'AI' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'createTime'">
          {{ formatDate(record.createTime) }}
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { adminListAppChatHistory } from '@/api/chatHistoryController'

const loading = ref(false)
const historyList = ref<API.ChatHistory[]>([])

const searchForm = reactive<API.ChatHistoryQueryRequest>({
  appId: undefined,
  userId: undefined,
  message: undefined,
  messageType: undefined,
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 100 },
  { title: '应用ID', dataIndex: 'appId', key: 'appId', width: 120 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 120 },
  { title: '消息类型', key: 'messageType', width: 100 },
  { title: '消息内容', dataIndex: 'message', key: 'message', ellipsis: true },
  { title: '创建时间', key: 'createTime', width: 180 },
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await adminListAppChatHistory({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      ...searchForm,
      sortField: 'createTime',
      sortOrder: 'descend',
    })
    if (res.data.code === 0 && res.data.data) {
      historyList.value = res.data.data.records || []
      pagination.total = Number(res.data.data.totalRow) || 0
    }
  } catch (e) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.appId = undefined
  searchForm.userId = undefined
  searchForm.message = undefined
  searchForm.messageType = undefined
  pagination.current = 1
  fetchData()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.admin-chat-page {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.search-bar {
  background: #fff;
  padding: 20px 24px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
</style>
