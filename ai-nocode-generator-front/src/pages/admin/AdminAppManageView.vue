<template>
  <div class="admin-app-page">
    <div class="page-header">
      <h2 class="page-title">应用管理</h2>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <a-form layout="inline" :model="searchForm">
        <a-form-item label="应用名称">
          <a-input
            v-model:value="searchForm.appName"
            placeholder="请输入应用名称"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="应用ID">
          <a-input
            v-model:value="searchForm.id"
            placeholder="请输入ID"
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
        <a-form-item label="代码生成类型">
          <a-input
            v-model:value="searchForm.codeGenType"
            placeholder="请输入类型"
            allow-clear
          />
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
      :data-source="appList"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      :scroll="{ x: 1200 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'cover'">
          <a-avatar
            v-if="record.cover"
            :src="record.cover"
            shape="square"
            :size="48"
          />
          <span v-else>-</span>
        </template>
        <template v-if="column.key === 'user'">
          <span v-if="record.user">{{ record.user.userName || record.user.userAccount }}</span>
          <span v-else>-</span>
        </template>
        <template v-if="column.key === 'priority'">
          <a-tag v-if="record.priority >= 99" color="gold">精选</a-tag>
          <span v-else>{{ record.priority ?? 0 }}</span>
        </template>
        <template v-if="column.key === 'createTime'">
          {{ formatDate(record.createTime) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(record)">
              编辑
            </a-button>
            <a-button
              type="link"
              size="small"
              @click="handleFeatured(record)"
              :disabled="record.priority >= 99"
            >
              精选
            </a-button>
            <a-popconfirm
              title="确定要删除此应用吗？"
              @confirm="handleDelete(record)"
            >
              <a-button type="link" danger size="small">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { listAppByPage, deleteAppByAdmin, updateAppByAdmin } from '@/api/appController'

const router = useRouter()
const loading = ref(false)
const appList = ref<API.AppVO[]>([])

const searchForm = reactive<API.AppQueryRequest>({
  appName: undefined,
  id: undefined,
  userId: undefined,
  codeGenType: undefined,
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '封面', key: 'cover', width: 80 },
  { title: '应用名称', dataIndex: 'appName', key: 'appName', width: 160, ellipsis: true },
  { title: '代码类型', dataIndex: 'codeGenType', key: 'codeGenType', width: 100 },
  { title: '优先级', key: 'priority', width: 100 },
  { title: '创建者', key: 'user', width: 120 },
  { title: '创建时间', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAppByPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      ...searchForm,
    })
    if (res.data.code === 0 && res.data.data) {
      appList.value = res.data.data.records || []
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
  searchForm.appName = undefined
  searchForm.id = undefined
  searchForm.userId = undefined
  searchForm.codeGenType = undefined
  pagination.current = 1
  fetchData()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const handleEdit = (record: API.AppVO) => {
  router.push(`/app/edit/${record.id}`)
}

const handleDelete = async (record: API.AppVO) => {
  try {
    const res = await deleteAppByAdmin({ id: record.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      fetchData()
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch (e) {
    message.error('删除失败')
  }
}

const handleFeatured = async (record: API.AppVO) => {
  try {
    const res = await updateAppByAdmin({ id: record.id, priority: 99 })
    if (res.data.code === 0) {
      message.success('已设为精选')
      fetchData()
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch (e) {
    message.error('操作失败')
  }
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
.admin-app-page {
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
