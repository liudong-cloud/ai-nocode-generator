import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getLoginUser } from '@/api/userController'

export const useUserStore = defineStore('user', () => {
  const loginUser = ref<API.LoginUserVO>({})

  async function fetchLoginUser() {
    try {
      const res = await getLoginUser()
      if (res.data.code === 0 && res.data.data) {
        loginUser.value = res.data.data
      }
    } catch (e) {
      // 未登录状态，清空用户信息
      loginUser.value = {}
    }
  }

  function setLoginUser(user: API.LoginUserVO) {
    loginUser.value = user
  }

  return { loginUser, fetchLoginUser, setLoginUser }
})
