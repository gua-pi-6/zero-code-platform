<template>
  <div id="userManagePage" class="page-shell page-shell--wide admin-page">
    <section class="admin-hero">
      <div>
        <span class="page-eyebrow">Admin Console</span>
        <h1 class="page-title page-title--section">用户管理</h1>
        <p class="page-subtitle">按账号或昵称检索当前系统用户，并执行必要的后台管理操作。</p>
      </div>
      <div class="admin-summary surface-panel surface-panel--compact">
        <span>当前结果</span>
        <strong>{{ total }}</strong>
      </div>
    </section>

    <section class="surface-panel admin-panel">
      <a-form layout="vertical" class="admin-filters" :model="searchParams" @finish="doSearch">
        <a-form-item label="账号">
          <a-input v-model:value="searchParams.userAccount" placeholder="输入账号关键字" />
        </a-form-item>
        <a-form-item label="昵称">
          <a-input v-model:value="searchParams.userName" placeholder="输入昵称关键字" />
        </a-form-item>
        <a-form-item class="admin-filters__action">
          <a-button type="primary" html-type="submit">搜索用户</a-button>
        </a-form-item>
      </a-form>

      <div class="admin-table">
        <a-table
          :columns="columns"
          :data-source="data"
          :pagination="pagination"
          @change="doTableChange"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'userAvatar'">
              <a-image :src="record.userAvatar" :width="96" :preview="false" />
            </template>
            <template v-else-if="column.dataIndex === 'userRole'">
              <span
                :class="[
                  'status-tag',
                  record.userRole === 'admin' ? 'status-tag--accent' : 'status-tag--success',
                ]"
              >
                {{ record.userRole === 'admin' ? '管理员' : '普通用户' }}
              </span>
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
              {{ formatTime(record.createTime) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button danger size="small" @click="doDelete(record.id)">删除</a-button>
            </template>
          </template>
        </a-table>
      </div>
    </section>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { deleteUser, listUserVoByPage } from '@/api/userController'
import { formatTime } from '@/utils/time'

const columns = [
  { title: 'ID', dataIndex: 'id' },
  { title: '账号', dataIndex: 'userAccount' },
  { title: '昵称', dataIndex: 'userName' },
  { title: '头像', dataIndex: 'userAvatar' },
  { title: '简介', dataIndex: 'userProfile' },
  { title: '角色', dataIndex: 'userRole' },
  { title: '创建时间', dataIndex: 'createTime' },
  { title: '操作', key: 'action' },
]

const data = ref<API.UserVO[]>([])
const total = ref(0)

const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

const fetchData = async () => {
  const res = await listUserVoByPage({
    ...searchParams,
  })
  if (res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error(`加载用户失败：${res.data.message}`)
  }
}

const pagination = computed(() => ({
  current: searchParams.pageNum ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 位用户`,
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

const doDelete = async (id: string | number | undefined) => {
  if (!id) {
    return
  }
  const res = await deleteUser({ id: String(id) })
  if (res.data.code === 0) {
    message.success('用户已删除')
    fetchData()
  } else {
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

@media (max-width: 900px) {
  .admin-hero {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
