<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'

const themeConfig = {
  token: {
    colorPrimary: '#141413',
    colorInfo: '#141413',
    colorSuccess: '#5e5d59',
    colorWarning: '#5e5d59',
    colorError: '#5e5d59',
    colorText: '#141413',
    colorTextSecondary: '#5e5d59',
    colorBgBase: '#ffffff',
    borderRadius: 18,
    fontFamily: "'Avenir Next', 'PingFang SC', 'Microsoft YaHei', sans-serif",
    boxShadow: '0 8px 24px rgba(20, 20, 19, 0.035)',
  },
  components: {
    Layout: {
      headerBg: 'transparent',
      footerBg: 'transparent',
      bodyBg: 'transparent',
    },
    Button: {
      controlHeight: 44,
      fontWeight: 600,
      defaultBorderColor: '#dfdfdc',
      defaultBg: '#ffffff',
      defaultColor: '#4d4c48',
      primaryShadow: 'none',
    },
    Input: {
      controlHeight: 48,
    },
    Select: {
      controlHeight: 48,
    },
    Card: {
      headerBg: 'transparent',
    },
    Table: {
      headerBg: '#ffffff',
    },
  },
}
</script>

<template>
  <a-config-provider :theme="themeConfig">
    <BasicLayout />
  </a-config-provider>
</template>

<style scoped>
:deep(.ant-app) {
  min-height: 100vh;
}
</style>
