<template>
  <div id="appManagePage" class="page-shell page-shell--wide admin-page">
    <section class="admin-hero">
      <div>
        <span class="page-eyebrow">Admin Console</span>
        <h1 class="page-title page-title--section">应用管理</h1>
        <p class="page-subtitle">查看平台应用、切换精选状态、进入编辑页，保持内容质量和展示优先级。</p>
      </div>
      <div class="admin-summary surface-panel surface-panel--compact">
        <span>应用总数</span>
        <strong>{{ total }}</strong>
      </div>
    </section>

    <section class="surface-panel admin-panel">
      <a-form layout="vertical" class="admin-filters" :model="searchParams" @finish="doSearch">
        <a-form-item label="应用名称">
          <a-input v-model:value="searchParams.appName" placeholder="输入应用名称" />
        </a-form-item>
        <a-form-item label="用户 ID">
          <a-input v-model:value="searchParams.userId" placeholder="输入创建者 ID" />
        </a-form-item>
        <a-form-item label="生成方式">
          <a-select v-model:value="searchParams.codeGenType" placeholder="选择生成方式">
            <a-select-option value="">全部</a-select-option>
            <a-select-option
              v-for="option in CODE_GEN_TYPE_OPTIONS"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item class="admin-filters__action">
          <a-button type="primary" html-type="submit">搜索应用</a-button>
        </a-form-item>
      </a-form>

      <div class="admin-table">
        <a-table
          :columns="columns"
          :data-source="data"
          :pagination="pagination"
          :scroll="{ x: 1200 }"
          @change="doTableChange"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'cover'">
              <a-image
                v-if="record.cover"
                :src="record.cover"
                :width="96"
                :height="72"
                :preview="false"
              />
              <div v-else class="no-cover">无封面</div>
            </template>
            <template v-else-if="column.dataIndex === 'initPrompt'">
              <a-tooltip :title="record.initPrompt">
                <div class="prompt-text">{{ record.initPrompt }}</div>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'codeGenType'">
              {{ formatCodeGenType(record.codeGenType) }}
            </template>
            <template v-else-if="column.dataIndex === 'priority'">
              <span
                :class="[
                  'status-tag',
                  record.priority === 99 ? 'status-tag--accent' : 'status-tag--warning',
                ]"
              >
                {{ record.priority === 99 ? '精选应用' : `优先级 ${record.priority || 0}` }}
              </span>
            </template>
            <template v-else-if="column.dataIndex === 'deployedTime'">
              {{ record.deployedTime ? formatTime(record.deployedTime) : '未部署' }}
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
              {{ formatTime(record.createTime) }}
            </template>
            <template v-else-if="column.dataIndex === 'user'">
              <UserInfo :user="record.user" size="small" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space wrap>
                <a-button type="primary" size="small" @click="editApp(record)">编辑</a-button>
                <a-button size="small" @click="toggleFeatured(record)">
                  {{ record.priority === 99 ? '取消精选' : '设为精选' }}
                </a-button>
                <a-popconfirm title="删除后将无法恢复，确定继续吗？" @confirm="deleteApp(record.id)">
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
import { deleteAppByAdmin, listAppVoByPageByAdmin, updateAppByAdmin } from '@/api/appController'
import { CODE_GEN_TYPE_OPTIONS, formatCodeGenType } from '@/utils/codeGenTypes'
import { hasId, toIdString } from '@/utils/id'
import { formatTime } from '@/utils/time'
import UserInfo from '@/components/UserInfo.vue'

const router = useRouter()

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80, fixed: 'left' },
  { title: '应用名称', dataIndex: 'appName', width: 160 },
  { title: '封面', dataIndex: 'cover', width: 120 },
  { title: '初始提示词', dataIndex: 'initPrompt', width: 220 },
  { title: '生成方式', dataIndex: 'codeGenType', width: 120 },
  { title: '优先级', dataIndex: 'priority', width: 120 },
  { title: '部署时间', dataIndex: 'deployedTime', width: 180 },
  { title: '创建者', dataIndex: 'user', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' },
]

const data = ref<API.AppVO[]>([])
const total = ref(0)

const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

const fetchData = async () => {
  try {
    const res = await listAppVoByPageByAdmin({
      ...searchParams,
    })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error(`加载应用失败：${res.data.message}`)
    }
  } catch (error) {
    console.error('加载应用失败', error)
    message.error('加载应用失败')
  }
}

const pagination = computed(() => ({
  current: searchParams.pageNum ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 个应用`,
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

const editApp = (app: API.AppVO) => {
  router.push(`/app/edit/${toIdString(app.id)}`)
}

const toggleFeatured = async (app: API.AppVO) => {
  if (!hasId(app.id)) return

  const newPriority = app.priority === 99 ? 0 : 99

  try {
    const res = await updateAppByAdmin({
      id: toIdString(app.id),
      priority: newPriority,
    })

    if (res.data.code === 0) {
      message.success(newPriority === 99 ? '已设为精选应用' : '已取消精选')
      fetchData()
    } else {
      message.error(`操作失败：${res.data.message}`)
    }
  } catch (error) {
    console.error('切换精选状态失败', error)
    message.error('操作失败')
  }
}

const deleteApp = async (id: string | number | undefined) => {
  if (!hasId(id)) return

  try {
    const res = await deleteAppByAdmin({ id: toIdString(id) })
    if (res.data.code === 0) {
      message.success('应用已删除')
      fetchData()
    } else {
      message.error(`删除失败：${res.data.message}`)
    }
  } catch (error) {
    console.error('删除应用失败', error)
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

.no-cover {
  display: grid;
  place-items: center;
  width: 96px;
  height: 72px;
  border-radius: 18px;
  color: var(--text-subtle);
  background: #ffffff;
}

.prompt-text {
  max-width: 220px;
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
