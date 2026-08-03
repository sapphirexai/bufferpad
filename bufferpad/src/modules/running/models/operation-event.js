export function normalizeSeverity(severity) {
  const value = String(severity || 'INFO').toUpperCase()
  if (value === 'ERROR') return 'ERROR'
  if (value === 'WARNING' || value === 'WARN') return 'WARNING'
  return 'INFO'
}

export function severityLabel(severity) {
  const value = normalizeSeverity(severity)
  if (value === 'ERROR') return '异常'
  if (value === 'WARNING') return '提醒'
  return '正常'
}

export function severityElementType(severity) {
  const value = normalizeSeverity(severity)
  if (value === 'ERROR') return 'danger'
  if (value === 'WARNING') return 'warning'
  return 'success'
}

export function isAttentionEvent(event) {
  return event && normalizeSeverity(event.severity) !== 'INFO'
}

export function prependOperationEvent(events, event, limit) {
  if (!event) return Array.isArray(events) ? events : []
  const source = Array.isArray(events) ? events : []
  const eventKey = event.eventId || event.id
  const filtered = eventKey === undefined || eventKey === null
    ? source
    : source.filter(item => (item.eventId || item.id) !== eventKey)
  return [event, ...filtered].slice(0, limit || 20)
}
