import axios from 'axios'
import { getConfiguredApiBaseUrl, normalizeApiBaseUrl } from '../config/backend'

export const http = axios.create({
  baseURL: normalizeApiBaseUrl(getConfiguredApiBaseUrl()),
  timeout: 60000,
  withCredentials: true
})

http.interceptors.request.use(config => {
  config.headers = {
    ...(config.headers || {}),
    DeviceType: 'H5'
  }
  return config
})

http.interceptors.response.use(
  response => response,
  error => Promise.reject(error)
)

export function buildBackendUrl(path) {
  const baseUrl = (http.defaults.baseURL || '').replace(/\/$/, '')
  const normalizedPath = path.indexOf('/') === 0 ? path : '/' + path
  return baseUrl ? baseUrl + normalizedPath : normalizedPath
}

export function request(config) {
  return http(config)
}

export function get(url, params) {
  return http.get(url, { params })
}

export function post(url, data, responseType) {
  return http({
    method: 'post',
    url,
    data,
    responseType
  })
}

export function del(url, params) {
  return http.delete(url, { params })
}

export function isSuccessResponse(res) {
  return res && res.status === 200 && res.data && res.data.codeSuccess
}

export function responseMessage(res, fallback) {
  return res && res.data && res.data.msg ? res.data.msg : (fallback || '操作失败')
}

export function responseData(res, fallback) {
  if (!isSuccessResponse(res)) return fallback
  return res.data.data === undefined ? fallback : res.data.data
}

export function pageRows(res) {
  const payload = responseData(res, null)
  return payload && Array.isArray(payload.data) ? payload.data : []
}

export function pageTotal(res) {
  const payload = responseData(res, null)
  if (!payload) return 0
  const total = payload.totalPage === undefined ? payload.total : payload.totalPage
  return Number(total || 0)
}

export function requestErrorMessage(error) {
  if (!error) return '请求失败'
  if (error.code === 'ECONNABORTED') return '请求超时，请稍后再试！'
  if (error.message === 'Network Error') return '网络连接异常，请检查您的网络设置！'
  if (error.response && error.response.data && error.response.data.msg) {
    return error.response.data.msg
  }
  return '发生错误：' + (error.message || '未知错误')
}
