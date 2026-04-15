<template>
  <div id="chatManagePage" class="page-shell page-shell--wide admin-page">
    <section class="admin-hero">
      <div>
        <span class="page-eyebrow">Admin Console</span>
        <h1 class="page-title page-title--section">对话管理</h1>
        <p class="page-subtitle">按消息内容、消息类型或应用 ID 检索历史对话，快速跳转回对应的聊天工作区。</p>
      </div>
      <div class="admin-summary surface-panel surface-panel--compact">
        <span>消息总数</span>
        <strong>{{ total }}</strong>
      </div>
    </section>

    <section class="surface-panel admin-panel">
      <a-form layout="vertical" class="admin-filters" :model="searchParams" @finish="doSearch">
        <a-form-item label="消息内容">
          <a-input v-model:value="searchParams.message" placeholder="输入消息关键字" />
        </a-form-item>
        <a-form-item label="消息类型">
          <a-select v-model:value="searchParams.messageType" placeholder="选择消息类型">
            <a-select-option value="">全部</a-select-option>
            <a-select-option value="user">用户消息</a-select-option>
            <a-select-option value="assistant">AI 消息</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="应用 ID">
          <a-input v-model:value="searchParams.appId" placeholder="输入应用 ID" />
        </a-form-item>
        <a-form-item label="用户 ID">
          <a-input v-model:value="searchParams.userId" placeholder="输入用户 ID" />
        </a-form-item>
        <a-form-item class="admin-filters__action">
          <a-button type="primary" html-type="submit">搜索消息</a-button>
        </a-form-item>
      </a-form>

      <div class="admin-table">
        <a-table
          :columns="columns"
          :data-source="data"
          :pagination="pagination"
          :scroll="{ x: 1280 }"
          @change="doTableChange"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'message'">
              <a-tooltip :title="record.message">
                <div class="message-text">{{ record.message }}</div>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'messageType'">
              <span
                :class="[
                  'status-tag',
                  record.messageType === 'user' ? 'status-tag--warning' : 'status-tag--success',
                ]"
              >
                {{ record.messageType === 'user' ? '用户消息' : 'AI 消息' }}
              </span>
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
              {{ formatTime(record.createTime) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space wrap>
                <a-button type="primary" size="small" @click="viewAppChat(record.appId)">
                  查看对话
                </a-button>
                <a-popconfirm title="确认删除这条消息吗？" @confirm="deleteMessage(record.id)">
                  <a-button danger size="small">删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </section>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { listAllChatHistoryByPageForAdmin } from '@/api/chatHistoryController'
import { hasId, toIdString } from '@/utils/id'
import { formatTime } from '@/utils/time'

const router = useRouter()

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80, fixed: 'left' },
  { title: '消息内容', dataIndex: 'message', width: 360 },
  { title: '消息类型', dataIndex: 'messageType', width: 120 },
  { title: '应用 ID', dataIndex: 'appId', width: 100 },
  { title: '用户 ID', dataIndex: 'userId', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' },
]

const data = ref<API.ChatHistory[]>([])
const total = ref(0)

const searchParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

const fetchData = async () => {
  try {
    const res = await listAllChatHistoryByPageForAdmin({
      ...searchParams,
    })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error(`加载消息失败：${res.data.message}`)
    }
  } catch (error) {
    console.error('加载消息失败', error)
    message.error('加载消息失败')
  }
}

const pagination = computed(() => ({
  current: searchParams.pageNum ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条消息`,
}))

const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

const viewAppChat = (appId: string | number | undefined) => {
  if (hasId(appId)) {
    router.push(`/app/chat/${toIdString(appId)}`)
  }
}

const deleteMessage = async (id: string | number | undefined) => {
  if (!hasId(id)) return

  try {
    message.success('消息已删除')
    fetchData()
  } catch (error) {
    console.error('删除消息失败', error)
    message.error('删除失败')
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.admin-page {
  display: grid;
  gap: 28px;
  padding-bottom: 40px;
}

.admin-hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
}

.admin-filters__action {
  align-self: end;
}

.message-text {
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .admin-hero {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
