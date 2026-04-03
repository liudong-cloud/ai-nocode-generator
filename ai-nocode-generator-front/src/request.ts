import JSONBig from 'json-bigint'
import axios, { type AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import CONFIG from '@/config'

/** 业务码：未登录（与后端约定一致） */
const NOT_LOGIN_CODE = 40100

function isUnauthorizedPayload(data: unknown): boolean {
  if (!data || typeof data !== 'object') return false
  const code = (data as { code?: unknown }).code
  return code === NOT_LOGIN_CODE || code === String(NOT_LOGIN_CODE)
}

function isUserLoginStatusRequest(config?: InternalAxiosRequestConfig): boolean {
  const url = String(config?.url ?? '')
  return url.includes('user/get/login')
}

function redirectToLoginPage() {
  if (window.location.pathname.includes('/user/login')) {
    return
  }
  message.warning('请先登录')
  const base = (import.meta.env.BASE_URL || '/').replace(/\/$/, '')
  const loginPath = base ? `${base}/user/login` : '/user/login'
  const redirect = encodeURIComponent(window.location.href)
  window.location.assign(`${loginPath}?redirect=${redirect}`)
}

function handleUnauthorizedResponse(response: AxiosResponse) {
  if (isUserLoginStatusRequest(response.config)) {
    return
  }
  redirectToLoginPage()
}

// 创建 Axios 实例
const myAxios = axios.create({
  baseURL: CONFIG.baseURL,
  timeout: CONFIG.timeout,
  withCredentials: CONFIG.withCredentials,
  transformResponse: [
    (data) => {
      try {
        return JSONBig({ storeAsString: true }).parse(data)
      } catch (e) {
        return data
      }
    },
  ],
})

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    // Do something before request is sent
    return config
  },
  function (error) {
    // Do something with request error
    return Promise.reject(error)
  },
)

// 全局响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    // HTTP 200 但业务体为未登录
    if (isUnauthorizedPayload(data)) {
      handleUnauthorizedResponse(response)
    }
    return response
  },
  function (error: AxiosError) {
    const res = error.response
    // HTTP 4xx/5xx 响应体中携带未登录业务码（部分网关或异常包装会走这里）
    if (res?.data !== undefined && isUnauthorizedPayload(res.data)) {
      handleUnauthorizedResponse(res as AxiosResponse)
    }
    return Promise.reject(error)
  },
)

export default myAxios
