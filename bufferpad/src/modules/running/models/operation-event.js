const SEVERITY_RANK = { INFO: 1, WARNING: 2, ERROR: 3 }

const CODE_PRIORITY = {
  SCAN_COUNT_FAILED: 100,
  CUSHION_MAX_REACHED: 95,
  SCAN_NO_READ: 90,
  SCAN_REPEATED: 85,
  PLC_WRITE_FAILED: 82,
  PLC_WRITE_REJECTED: 82,
  PLC_OFFLINE: 80,
  PLC_TARGET_NOT_RESOLVED: 76,
  PLC_ADDRESS_NOT_CONFIGURED: 74,
  PLC_READ_ADDRESS_NOT_CONFIGURED: 72,
  PLC_READ_FAILED: 70,
  SCAN_COUNTED: 60,
  PLC_NOTIFY_SUCCEEDED: 20,
  HTTP_SCAN_RESULT: 10
}

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

export function operationEventKey(event) {
  if (!event) return ''
  return event.operationId || event.eventId || event.id || [event.code, event.occurredAt].join('-')
}

function eventIdentity(event) {
  if (!event) return ''
  return event.eventId || event.id || [event.code, event.message, event.occurredAt].join('|')
}

function eventScore(event) {
  return (SEVERITY_RANK[normalizeSeverity(event && event.severity)] || 1) * 1000 +
    (CODE_PRIORITY[event && event.code] || 0)
}

function newestTime(events) {
  return events.reduce((latest, event) => {
    const time = new Date(event.occurredAt || 0).getTime()
    return Number.isNaN(time) ? latest : Math.max(latest, time)
  }, 0)
}

export function buildOperationFeedback(events) {
  const source = Array.isArray(events) ? events.filter(Boolean) : []
  if (source.length === 0) return null
  const sorted = source.slice().sort((left, right) => eventScore(right) - eventScore(left))
  const primary = sorted[0]
  const secondary = sorted.find(item => item.message && item.message !== primary.message)
  const severity = source.reduce((value, item) => {
    return SEVERITY_RANK[normalizeSeverity(item.severity)] > SEVERITY_RANK[value]
      ? normalizeSeverity(item.severity)
      : value
  }, 'INFO')
  const occurredAt = newestTime(source)

  return {
    ...primary,
    operationId: operationEventKey(primary),
    severity,
    title: primary.title || '操作结果',
    message: primary.message || '操作已完成',
    secondaryMessage: secondary ? secondary.message : '',
    occurredAt: occurredAt ? new Date(occurredAt).toISOString() : primary.occurredAt,
    eventCount: source.length,
    events: source.slice().sort((left, right) => {
      return new Date(right.occurredAt || 0).getTime() - new Date(left.occurredAt || 0).getTime()
    })
  }
}

export function groupOperationEvents(events, limit) {
  const groups = []
  const indexes = {}
  const source = Array.isArray(events) ? events : []

  source.forEach(event => {
    const key = operationEventKey(event)
    if (indexes[key] === undefined) {
      indexes[key] = groups.length
      groups.push([])
    }
    groups[indexes[key]].push(event)
  })

  return groups
    .map(buildOperationFeedback)
    .filter(Boolean)
    .sort((left, right) => new Date(right.occurredAt || 0).getTime() - new Date(left.occurredAt || 0).getTime())
    .slice(0, limit || 20)
}

export function prependOperationEvent(events, event, limit) {
  if (!event) return Array.isArray(events) ? events : []
  const source = Array.isArray(events) ? events : []
  const eventKey = eventIdentity(event)
  const filtered = eventKey
    ? source.filter(item => eventIdentity(item) !== eventKey)
    : source
  return [event, ...filtered].slice(0, limit || 60)
}
