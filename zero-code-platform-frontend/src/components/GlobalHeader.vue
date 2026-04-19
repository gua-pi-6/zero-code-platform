<template>
  <a-layout-header
    :class="[
      'header',
      {
        'header--compact': compact,
        'header--home': isHomePage,
      },
    ]"
  >
    <div class="header-shell page-shell--wide">
      <RouterLink class="brand" to="/">
        <img class="logo" src="@/assets/logo.png" alt="零代码应用平台" />
        <div class="brand-copy">
          <span class="brand-kicker">Zero-Code Studio</span>
          <strong class="brand-title">NoCode</strong>
        </div>
      </RouterLink>

      <nav v-if="navItems.length" class="nav-links" aria-label="主导航">
        <button
          v-for="item in navItems"
          :key="item.key"
          type="button"
          :class="['nav-link', { 'nav-link--active': isActive(item) }]"
          @click="handleNavigate(item)"
        >
          {{ item.label }}
        </button>
      </nav>

      <div class="header-actions">
        <span v-if="isAdmin && !isHomePage" class="role-chip">Admin</span>

        <template v-if="hasLoginUser">
          <a-dropdown placement="bottomRight">
            <button type="button" class="user-chip">
              <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="36">
                {{ loginUserStore.loginUser.userName?.charAt(0) || 'U' }}
              </a-avatar>
              <span class="user-chip__text">
                {{ loginUserStore.loginUser.userName || '当前用户' }}
              </span>
            </button>
            <template #overlay>
              <a-menu>
                <a-menu-item key="logout" @click="doLogout">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>

        <template v-else>
          <a-button type="primary" class="login-button" @click="router.push('/user/login')">
            登录
          </a-button>
        </template>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LogoutOutlined } from '@ant-design/icons-vue'
import { userLogout } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'
import { hasId } from '@/utils/id'

interface Props {
  compact?: boolean
}

interface NavItem {
  key: string
  label: string
  href?: string
  external?: boolean
}

withDefaults(defineProps<Props>(), {
  compact: false,
})

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
const hasLoginUser = computed(() => hasId(loginUserStore.loginUser.id))
const isHomePage = computed(() => route.path === '/')
const canShowAdminNav = computed(() => hasLoginUser.value && isAdmin.value)

const adminNavItems: NavItem[] = [
  {
    key: '/admin/appManage',
    label: '应用管理',
  },
  {
    key: '/admin/chatManage',
    label: '对话管理',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
  },
]

const navItems = computed<NavItem[]>(() => {
  if (isHomePage.value) {
    return canShowAdminNav.value ? adminNavItems : []
  }

  const items: NavItem[] = [
    {
      key: '/',
      label: '首页',
    },
  ]

  if (canShowAdminNav.value) {
    items.push(...adminNavItems)
  }

  return items
})

const isActive = (item: NavItem) =>
  !item.external && (route.path === item.key || route.path.startsWith(`${item.key}/`))

const handleNavigate = async (item: NavItem) => {
  if (item.external && item.href) {
    window.open(item.href, '_blank', 'noopener,noreferrer')
    return
  }

  if (route.path !== item.key) {
    await router.push(item.key)
  }
}

const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('已退出登录')
    await router.push('/user/login')
  } else {
    message.error(`退出失败：${res.data.message}`)
  }
}
</script>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 30;
  height: auto;
  padding: 12px 0 8px;
}

.header--compact {
  padding-bottom: 6px;
}

.header-shell {
  position: relative;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 28px;
  align-items: center;
  width: var(--content-width-wide);
  margin: 0 auto;
  padding: 12px 22px;
  border: 1px solid rgba(20, 20, 19, 0.08);
  border-radius: 26px;
  background: #ffffff;
  box-shadow:
    0 18px 50px rgba(20, 20, 19, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.56);
  backdrop-filter: saturate(180%) blur(22px);
  overflow: hidden;
}

.header-shell::before,
.header-shell::after {
  content: none;
}

.header--home {
  padding: 0;
}

.header--home :deep(.page-shell--wide) {
  width: 100%;
}

.brand {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.logo {
  display: block;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: transparent;
  box-shadow: none;
  object-fit: cover;
}

.brand-copy {
  display: grid;
  gap: 4px;
}

.brand-kicker {
  color: rgba(94, 93, 89, 0.72);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.header--home .brand-kicker {
  display: none;
}

.brand-title {
  color: var(--text-strong);
  font-family: var(--font-sans);
  font-size: 1.9rem;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.03em;
}

.nav-links {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  justify-content: start;
  gap: 36.8px;
  margin-left: 8.8px;
}

.nav-link {
  min-height: auto;
  padding: 0;
  border: 0;
  border-radius: 0;
  color: #5f5b54;
  background: transparent;
  font-size: 0.9rem;
  font-weight: 500;
  line-height: 1.4;
  cursor: pointer;
  transition:
    color 0.2s ease,
    opacity 0.2s ease;
}

.nav-link:hover,
.nav-link--active {
  color: #141413;
  background: transparent;
}

.header-actions {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: end;
  gap: 12px;
}

.header--home .header-shell {
  width: 100%;
  margin: 0;
  min-height: 74px;
  padding: 12px clamp(140.4px, 15.6vw, 218.4px) 10px;
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
  overflow: visible;
}

.header--home .header-shell::before,
.header--home .header-shell::after {
  content: none;
}

.header--home .header-actions {
  padding-right: 0;
}

.login-button {
  min-width: 84px;
  border-color: rgba(20, 20, 19, 0.08);
  background: rgba(20, 20, 19, 0.86);
}

.login-button:hover:not(:disabled),
.login-button:focus:not(:disabled) {
  background: #141413;
}

.role-chip {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  color: var(--text-muted);
  background: rgba(20, 20, 19, 0.06);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 4px 12px 4px 4px;
  border: 1px solid rgba(20, 20, 19, 0.08);
  border-radius: 999px;
  color: var(--text-strong);
  background: #ffffff;
  cursor: pointer;
}

.header--home .user-chip {
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.user-chip__text {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .header-shell {
    grid-template-columns: 1fr;
    justify-items: stretch;
  }

  .nav-links {
    justify-content: start;
  }

  .header-actions {
    justify-content: start;
    flex-wrap: wrap;
  }

  .header--home .header-actions {
    padding-right: 0;
  }
}

@media (max-width: 768px) {
  .header {
    padding-top: 12px;
  }

  .header-shell {
    gap: 16px;
    padding: 14px 16px;
    border-radius: 22px;
  }

  .header--home :deep(.page-shell--wide) {
    width: 100%;
  }

  .header--home .header-shell {
    min-height: 64px;
    padding: 10px 70.2px 8px;
    border-radius: 0;
  }

  .brand-title {
    font-size: 1.5rem;
  }
}
</style>
