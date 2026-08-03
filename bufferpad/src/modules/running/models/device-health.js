const STATE_PRIORITY = {
  OFFLINE: 0,
  DEGRADED: 1,
  CONNECTING: 2,
  ONLINE: 3
}

export function deviceStateCode(device) {
  if (device && device.statusCode) return String(device.statusCode).toUpperCase()
  return device && Number(device.status) === 1 ? 'ONLINE' : 'OFFLINE'
}

export function deviceStateLabel(device) {
  const labels = {
    ONLINE: '在线',
    CONNECTING: '连接中',
    DEGRADED: '通信异常',
    OFFLINE: '离线'
  }
  return labels[deviceStateCode(device)] || '未知'
}

export function deviceFallbackReason(device) {
  return deviceStateCode(device) === 'ONLINE' ? '连接正常' : '设备未连接'
}

export function isDeviceAttentionRequired(device) {
  return deviceStateCode(device) !== 'ONLINE'
}

export function sortDevicesByHealth(devices) {
  return (Array.isArray(devices) ? devices : []).slice().sort((left, right) => {
    const stateDifference = (STATE_PRIORITY[deviceStateCode(left)] || 0) -
      (STATE_PRIORITY[deviceStateCode(right)] || 0)
    if (stateDifference !== 0) return stateDifference
    return String(left.name || '').localeCompare(String(right.name || ''))
  })
}

export function summarizeDevices(devices) {
  const list = Array.isArray(devices) ? devices : []
  const counts = list.reduce((result, device) => {
    const state = deviceStateCode(device)
    result[state] = (result[state] || 0) + 1
    return result
  }, { ONLINE: 0, CONNECTING: 0, DEGRADED: 0, OFFLINE: 0 })

  return {
    total: list.length,
    online: counts.ONLINE,
    connecting: counts.CONNECTING,
    degraded: counts.DEGRADED,
    offline: counts.OFFLINE,
    attention: list.length - counts.ONLINE,
    state: list.length === 0
      ? 'UNCONFIGURED'
      : (counts.OFFLINE > 0 || counts.DEGRADED > 0
          ? 'ERROR'
          : (counts.CONNECTING > 0 ? 'WARNING' : 'HEALTHY'))
  }
}

export function summarizeSystem(plcDevices, scannerDevices) {
  const plc = summarizeDevices(plcDevices)
  const scanner = summarizeDevices(scannerDevices)
  const total = plc.total + scanner.total
  const attention = plc.attention + scanner.attention

  return {
    total,
    attention,
    state: total === 0
      ? 'UNCONFIGURED'
      : (attention === 0
          ? 'HEALTHY'
          : (plc.state === 'ERROR' || scanner.state === 'ERROR' ? 'ERROR' : 'WARNING'))
  }
}
