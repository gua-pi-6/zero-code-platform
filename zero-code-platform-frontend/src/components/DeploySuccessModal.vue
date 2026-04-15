<template>
  <a-modal v-model:open="visible" title="部署成功" :footer="null" width="620px">
    <div class="deploy-modal">
      <div class="deploy-state">
        <div class="deploy-state__icon">
          <CheckCircleOutlined />
        </div>
        <div>
          <h3>站点已经发布完成</h3>
          <p>当前部署地址已经可用，你可以直接复制链接，或立即在新标签页中打开站点。</p>
        </div>
      </div>

      <div class="deploy-url">
        <a-input :value="deployUrl" readonly>
          <template #suffix>
            <a-button type="text" @click="handleCopyUrl">
              <CopyOutlined />
            </a-button>
          </template>
        </a-input>
      </div>

      <div class="deploy-actions">
        <a-button type="primary" @click="handleOpenSite">打开站点</a-button>
        <a-button @click="handleClose">稍后再说</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { CheckCircleOutlined, CopyOutlined } from '@ant-design/icons-vue'

interface Props {
  open: boolean
  deployUrl: string
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'open-site'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const handleCopyUrl = async () => {
  try {
    await navigator.clipboard.writeText(props.deployUrl)
    message.success('部署地址已复制到剪贴板')
  } catch (error) {
    console.error('复制部署地址失败', error)
    message.error('复制失败，请稍后重试')
  }
}

const handleOpenSite = () => {
  emit('open-site')
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.deploy-modal {
  display: grid;
  gap: 22px;
}

.deploy-state {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 16px;
  align-items: start;
}

.deploy-state__icon {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: 18px;
  color: var(--success);
  background: rgba(93, 122, 91, 0.12);
  font-size: 28px;
}

.deploy-state h3 {
  margin: 0;
  color: var(--text-strong);
  font-family: var(--font-serif);
  font-size: 1.8rem;
}

.deploy-state p {
  margin: 10px 0 0;
  color: var(--text-muted);
  line-height: 1.8;
}

.deploy-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
