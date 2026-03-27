import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../pages/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/user/login',
      name: 'userLogin',
      component: () => import('@/pages/user/UserLoginView.vue'),
      meta: { hideLayout: true },
    },
    {
      path: '/user/register',
      name: 'userRegister',
      component: () => import('@/pages/user/UserRegisterView.vue'),
      meta: { hideLayout: true },
    },
    {
      path: '/app/chat/:id',
      name: 'appChat',
      component: () => import('@/pages/app/AppChatView.vue'),
    },
    {
      path: '/app/edit/:id',
      name: 'appEdit',
      component: () => import('@/pages/app/AppEditView.vue'),
    },
    {
      path: '/user/profile',
      name: 'userProfile',
      component: () => import('@/pages/user/UserProfileView.vue'),
    },
    {
      path: '/admin/app',
      name: 'adminApp',
      component: () => import('@/pages/admin/AdminAppManageView.vue'),
    },
    {
      path: '/admin/chat',
      name: 'adminChat',
      component: () => import('@/pages/admin/AdminChatHistoryManageView.vue'),
    },
  ],
})

export default router

import { useUserStore } from '@/stores/user'

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  // 仅在未加载过用户信息时加载
  if (!userStore.loginUser.id) {
    await userStore.fetchLoginUser()
  }

  // 简单权限控制：如果去个人中心但没登录，跳登录页
  if (to.path === '/user/profile' && !userStore.loginUser.id) {
    next({
      path: '/user/login',
      query: { redirect: to.fullPath },
    })
    return
  }

  next()
})
