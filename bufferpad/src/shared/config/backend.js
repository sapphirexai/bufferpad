export function getRuntimeConfig() {
  if (typeof window === 'undefined') return {}
  return window.__APP_CONFIG__ || {}
}

export function normalizeApiBaseUrl(raw) {
  let baseUrl = (raw || '/api').trim()

  if (baseUrl.indexOf('/') === 0) {
    return baseUrl.replace(/\/$/, '')
  }

  if (!/^https?:\/\//i.test(baseUrl)) {
    baseUrl = 'http://' + baseUrl
  }

  return baseUrl.replace(/\/api\/?$/i, '').replace(/\/$/, '')
}

export function getConfiguredApiBaseUrl() {
  const runtimeConfig = getRuntimeConfig()
  return runtimeConfig.API_BASE_URL || runtimeConfig.apiBaseUrl || process.env.BASE_URL
}
