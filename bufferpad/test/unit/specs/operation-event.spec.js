import {
  isAttentionEvent,
  normalizeSeverity,
  prependOperationEvent,
  severityElementType,
  severityLabel
} from '@/modules/running/models/operation-event'

describe('operation event helpers', () => {
  it('normalizes backend severity values', () => {
    expect(normalizeSeverity('warn')).toBe('WARNING')
    expect(normalizeSeverity('ERROR')).toBe('ERROR')
    expect(normalizeSeverity(null)).toBe('INFO')
    expect(severityLabel('WARNING')).toBe('提醒')
    expect(severityElementType('ERROR')).toBe('danger')
  })

  it('deduplicates events and keeps newest first', () => {
    const oldEvent = { eventId: '1', title: '旧记录' }
    const nextEvent = { eventId: '2', title: '新记录' }
    const updatedOldEvent = { eventId: '1', title: '更新后的记录' }

    const events = prependOperationEvent([oldEvent], nextEvent, 20)
    expect(events.map(item => item.eventId)).toEqual(['2', '1'])

    const deduplicated = prependOperationEvent(events, updatedOldEvent, 20)
    expect(deduplicated).toHaveLength(2)
    expect(deduplicated[0].title).toBe('更新后的记录')
  })

  it('identifies events that require operator attention', () => {
    expect(isAttentionEvent({ severity: 'ERROR' })).toBe(true)
    expect(isAttentionEvent({ severity: 'WARNING' })).toBe(true)
    expect(isAttentionEvent({ severity: 'INFO' })).toBe(false)
  })
})
