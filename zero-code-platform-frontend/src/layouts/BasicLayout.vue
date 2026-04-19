<template>
  <a-layout
    :class="[
      'basic-layout',
      {
        'basic-layout--workspace': isWorkspacePage,
        'basic-layout--home': isHomePage,
      },
    ]"
  >
    <GlobalHeader v-if="!isWorkspacePage" :compact="isWorkspacePage" />
    <a-layout-content
      :class="[
        'main-content',
        {
          'main-content--workspace': isWorkspacePage,
          'main-content--home': isHomePage,
        },
      ]"
    >
      <router-view />
    </a-layout-content>
    <GlobalFooter v-if="showFooter" />
  </a-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import GlobalFooter from '@/components/GlobalFooter.vue'
import GlobalHeader from '@/components/GlobalHeader.vue'

const route = useRoute()

const isWorkspacePage = computed(() => route.path.startsWith('/app/chat/'))
const isHomePage = computed(() => route.path === '/')
const showFooter = computed(() => !isWorkspacePage.value && !isHomePage.value)
</script>

<style scoped>
.basic-layout {
  position: relative;
  min-height: 100vh;
}

.main-content {
  position: relative;
  z-index: 1;
  width: 100%;
  min-height: calc(100vh - var(--header-height));
  padding-top: 18px;
}

.main-content--workspace {
  min-height: 100vh;
  padding-top: 0;
}

.main-content--home {
  padding-top: 0;
}

@media (max-width: 768px) {
  .main-content {
    padding-top: 14px;
  }

  .main-content--home {
    padding-top: 0;
  }
}
</style>
