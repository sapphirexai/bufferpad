import { buildOperationFeedback, operationEventKey } from '../models/operation-event'

const TERMINAL_CODES = {
  PLC_TARGET_NOT_RESOLVED: true,
  PLC_ADDRESS_NOT_CONFIGURED: true,
  PLC_READ_ADDRESS_NOT_CONFIGURED: true,
  PLC_OFFLINE: true,
  PLC_NOTIFY_SUCCEEDED: true,
  PLC_WRITE_FAILED: true,
  PLC_WRITE_REJECTED: true,
  PLC_READ_FAILED: true
}

export function createOperationFeedbackCenter(options) {
  const config = options || {}
  const delay = config.delay === undefined ? 1500 : config.delay
  const terminalDelay = config.terminalDelay === undefined ? 180 : config.terminalDelay
  const onFeedback = typeof config.onFeedback === 'function' ? config.onFeedback : () => {}
  const operations = {}
  const seenEventIds = {}

  function emit(entry, showNotice) {
    const feedback = buildOperationFeedback(entry.events)
    if (feedback) onFeedback(feedback, showNotice)
  }

  function schedule(entry, event) {
    if (entry.notified) return
    if (entry.timer) clearTimeout(entry.timer)
    const wait = TERMINAL_CODES[event && event.code] ? terminalDelay : delay
    entry.timer = setTimeout(() => {
      entry.timer = null
      entry.notified = true
      emit(entry, true)
    }, wait)
  }

  function ingest(event, notify) {
    if (!event) return false
    const eventId = event.eventId || event.id
    if (eventId && seenEventIds[eventId]) return false
    if (eventId) seenEventIds[eventId] = true

    const operationId = operationEventKey(event)
    if (!operations[operationId]) {
      operations[operationId] = { events: [], notified: false, timer: null }
    }
    const entry = operations[operationId]
    entry.events.push(event)
    emit(entry, false)
    if (notify !== false) schedule(entry, event)
    return true
  }

  function seed(events) {
    const source = Array.isArray(events) ? events : []
    source.slice().reverse().forEach(event => ingest(event, false))
  }

  function destroy() {
    Object.keys(operations).forEach(key => {
      if (operations[key].timer) clearTimeout(operations[key].timer)
    })
  }

  return { ingest, seed, destroy }
}
