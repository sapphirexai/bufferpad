export function hasCurrentCushion(qrCode) {
  const normalized = String(qrCode || '').trim()
  return normalized !== '' && normalized !== '-' && normalized.toLowerCase() !== 'noread'
}

export function currentCushionText(qrCode) {
  const normalized = String(qrCode || '').trim()
  if (normalized.toLowerCase() === 'noread') return '扫码失败'
  return hasCurrentCushion(normalized) ? normalized : '等待扫码'
}

export function buildUsageMetrics(qrCode, usedCount, maxUseCount) {
  if (!hasCurrentCushion(qrCode)) {
    return {
      hasCurrent: false,
      usedCount: null,
      maxUseCount: null,
      remainingCount: null,
      percentage: 0
    }
  }

  const used = Math.max(0, Number(usedCount) || 0)
  const maximum = Math.max(0, Number(maxUseCount) || 0)

  return {
    hasCurrent: true,
    usedCount: used,
    maxUseCount: maximum,
    remainingCount: Math.max(0, maximum - used),
    percentage: maximum > 0 ? Math.min(100, Math.round(used / maximum * 100)) : 0
  }
}
