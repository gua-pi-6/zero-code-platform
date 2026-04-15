<template>
  <div class="user-info">
    <a-avatar class="user-avatar" :src="user?.userAvatar" :size="size">
      {{ user?.userName?.charAt(0) || 'U' }}
    </a-avatar>
    <div v-if="showName" class="user-meta">
      <span class="user-name">{{ user?.userName || '未命名用户' }}</span>
      <span v-if="user?.userRole" class="user-role">
        {{ user.userRole === 'admin' ? '管理员' : '创作者' }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  user?: API.UserVO
  size?: number | 'small' | 'default' | 'large'
  showName?: boolean
}

withDefaults(defineProps<Props>(), {
  size: 'default',
  showName: true,
})
</script>

<style scoped>
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  box-shadow: 0 6px 16px rgba(20, 20, 19, 0.08);
}

.user-meta {
  display: grid;
  gap: 2px;
}

.user-name {
  color: var(--text-strong);
  font-size: 14px;
}

.user-role {
  color: var(--text-subtle);
  font-size: 12px;
}
</style>
