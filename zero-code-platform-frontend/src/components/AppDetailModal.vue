<template>
  <a-modal v-model:open="visible" title="应用概览" :footer="null" width="560px">
    <div class="detail-modal">
      <section class="detail-hero">
        <span class="page-eyebrow">App Profile</span>
        <h2>{{ app?.appName || '未命名应用' }}</h2>
        <p>{{ promptSummary }}</p>
      </section>

      <section class="detail-grid">
        <div class="detail-item">
          <span>创建者</span>
          <div><UserInfo :user="app?.user" size="small" /></div>
        </div>
        <div class="detail-item">
          <span>创建时间</span>
          <strong>{{ formatTime(app?.createTime) || '暂无记录' }}</strong>
        </div>
        <div class="detail-item">
          <span>生成方式</span>
          <strong>{{ formatCodeGenType(app?.codeGenType) }}</strong>
        </div>
        <div class="detail-item">
          <span>部署状态</span>
          <strong>{{ app?.deployKey ? '已发布' : '未部署' }}</strong>
        </div>
      </section>

      <section v-if="showActions" class="detail-actions">
        <a-button type="primary" @click="handleEdit">
          <template #icon>
            <EditOutlined />
          </template>
          编辑应用
        </a-button>
        <a-popconfirm
          title="删除后将无法恢复，确定继续吗？"
          ok-text="确认删除"
          cancel-text="取消"
          @confirm="handleDelete"
        >
          <a-button danger>
            <template #icon>
              <DeleteOutlined />
            </template>
            删除应用
          </a-button>
        </a-popconfirm>
      </section>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { DeleteOutlined, EditOutlined } from '@ant-design/icons-vue'
import UserInfo from '@/components/UserInfo.vue'
import { formatTime } from '@/utils/time'
import { formatCodeGenType } from '@/utils/codeGenTypes'

interface Props {
  open: boolean
  app?: API.AppVO
  showActions?: boolean
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'edit'): void
  (e: 'delete'): void
}

const props = withDefaults(defineProps<Props>(), {
  showActions: false,
})

const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const promptSummary = computed(() => {
  const prompt = props.app?.initPrompt?.trim()
  if (!prompt) {
    return '这是一个等待继续完善的应用项目，你可以继续对话、编辑或部署它。'
  }
  return prompt.length > 150 ? `${prompt.slice(0, 150)}...` : prompt
})

const handleEdit = () => {
  emit('edit')
}

const handleDelete = () => {
  emit('delete')
}
</script>

<style scoped>
.detail-modal {
  display: grid;
  gap: 24px;
}

.detail-hero h2 {
  margin: 0;
  color: var(--text-strong);
  font-family: var(--font-serif);
  font-size: 2rem;
  line-height: 1.1;
}

.detail-hero p {
  margin: 14px 0 0;
  color: var(--text-muted);
  line-height: 1.8;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.detail-item {
  padding: 18px;
  border: 1px solid var(--border-light);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.56);
}

.detail-item span {
  display: block;
  margin-bottom: 10px;
  color: var(--text-subtle);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.detail-item strong {
  color: var(--text-strong);
  font-size: 15px;
  font-weight: 600;
}

.detail-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

@media (max-width: 640px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
