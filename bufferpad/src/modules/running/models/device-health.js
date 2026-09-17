const FAULTS = ['OFFLINE', 'DEGRADED', 'TIMEOUT']
const PRIORITY = { OFFLINE: 0, TIMEOUT: 0, DEGRADED: 1, UNKNOWN: 2, RETRYING: 3, CONNECTING: 3, VERIFYING: 4, ONLINE: 5 }

export function deviceStateCode(device) {
  if (device && device.communicationState === 'TIMEOUT') return 'TIMEOUT'
  if (device && device.statusCode) return String(device.statusCode).toUpperCase()
  // Legacy numeric status proves transport availability only.
  return device && Number(device.status) === 1 ? 'VERIFYING' : 'OFFLINE'
}
export function deviceTransportState(device) {
  if (device && device.transportState) return device.transportState
  const state = deviceStateCode(device)
  if (state === 'UNKNOWN') return 'UNKNOWN'
  if (state === 'CONNECTING') return 'CONNECTING'
  return device && Number(device.status) === 1 || ['ONLINE', 'VERIFYING', 'DEGRADED', 'RETRYING'].includes(state)
    ? 'CONNECTED' : 'DISCONNECTED'
}
export function deviceStateLabel(device) {
  return ({ ONLINE: '最近通信成功', VERIFYING: '通信未验证', UNKNOWN: '状态未知',
    CONNECTING: '连接中', RETRYING: '应答重试中', TIMEOUT: '通信超时', DEGRADED: '读写异常', OFFLINE: '连接已断开' })[deviceStateCode(device)] || '状态未知'
}
export function deviceFallbackReason(device) {
  return ({ ONLINE: '最近通信成功，成功时间不代表持续有数据', VERIFYING: 'TCP已连接，尚无有效报文；允许正常待机',
    UNKNOWN: '暂时无法获取设备状态', CONNECTING: '正在连接', RETRYING: '请求暂未应答，尚未达到故障阈值',
    TIMEOUT: '已发送请求连续超时', DEGRADED: '设备已应答，但读写未成功', OFFLINE: 'TCP连接失败或已断开' })[deviceStateCode(device)] || '状态未知'
}
export function isDeviceAttentionRequired(device) { return FAULTS.includes(deviceStateCode(device)) }
export function sortDevicesByHealth(devices) {
  return (devices || []).slice().sort((a, b) => (PRIORITY[deviceStateCode(a)] || 0) - (PRIORITY[deviceStateCode(b)] || 0) || String(a.name || '').localeCompare(String(b.name || '')))
}
export function summarizeDevices(devices) {
  const list = Array.isArray(devices) ? devices : []
  const count = state => list.filter(d => deviceStateCode(d) === state).length
  const attention = list.filter(isDeviceAttentionRequired).length
  const unverified = count('VERIFYING'), unknown = count('UNKNOWN')
  const connecting = count('CONNECTING'), retrying = count('RETRYING')
  return { total: list.length, online: count('ONLINE'), connected: list.filter(d => deviceTransportState(d) === 'CONNECTED').length,
    unverified, unknown, connecting, retrying, degraded: count('DEGRADED'), offline: count('OFFLINE'), timeout: count('TIMEOUT'), attention,
    state: !list.length ? 'UNCONFIGURED' : attention ? 'ERROR' : (unknown || connecting || retrying) ? 'WARNING' : unverified ? 'UNVERIFIED' : 'HEALTHY' }
}
export function summarizeSystem(plcDevices, scannerDevices) { return summarizeDevices([...(plcDevices || []), ...(scannerDevices || [])]) }
export function unknownDeviceStatuses(devices) {
  return (devices || []).map(item => ({ ...item, status: 0, statusCode: 'UNKNOWN', transportState: 'UNKNOWN', communicationState: 'UNKNOWN',
    statusReason: '实时状态暂不可用，正在重新获取', lastErrorCode: null }))
}
