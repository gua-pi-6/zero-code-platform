import { message } from 'ant-design-vue'
import router from '@/router'
import { useLoginUserStore } from '@/stores/loginUser'

let firstFetchLoginUser = true

router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser

  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }

  if (to.fullPath.startsWith('/admin')) {
    if (!loginUser || loginUser.userRole !== 'admin') {
      message.error('需要管理员权限才能访问该页面')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }

  next()
})
