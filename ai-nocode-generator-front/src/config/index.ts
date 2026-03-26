/**
 * 全局配置信息
 * 
 * 将 .env 环境变量通过此对象暴露出来，支持类型转换和默认值。
 * 项目中建议始终通过这个 CONFIG 对象来读取环境变量，便于后续维护。
 */
const CONFIG = {
  // 基础请求路径 (从 .env 读取 VITE_API_BASE_URL)
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api',
  
  // 请求超时时间 (从 .env 读取并转为数字)
  timeout: Number(import.meta.env.VITE_API_TIMEOUT) || 60000,
  
  // 网站标题 (从 .env 读取 VITE_APP_TITLE)
  title: import.meta.env.VITE_APP_TITLE || 'AI 低代码生成器',
  
  // 是否携带凭证 (默认 true)
  withCredentials: true,
};

export default CONFIG;
