import {
  buildOperationFeedback,
  groupOperationEvents,
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

  it('groups one scan and its PLC outcome into one operation', () => {
    const events = [
      {
        eventId: 'event-2',
        operationId: 'operation-1',
        code: 'PLC_OFFLINE',
        severity: 'ERROR',
        title: 'PLC未连接',
        message: '缓冲垫已计数，但PLC当前不可用',
        occurredAt: '2026-08-03T12:00:01Z'
      },
      {
        eventId: 'event-1',
        operationId: 'operation-1',
        code: 'SCAN_COUNTED',
        severity: 'INFO',
        title: '扫码计数完成',
        message: '当前使用26次',
        occurredAt: '2026-08-03T12:00:00Z'
      }
    ]

    const groups = groupOperationEvents(events, 20)

    expect(groups).toHaveLength(1)
    expect(groups[0].severity).toBe('ERROR')
    expect(groups[0].eventCount).toBe(2)
    expect(groups[0].title).toBe('PLC未连接')
    expect(groups[0].secondaryMessage).toBe('当前使用26次')
  })

  it('selects the repeated-scan result as the operator-facing conclusion', () => {
    const feedback = buildOperationFeedback([
      { operationId: 'operation-2', code: 'PLC_TARGET_NOT_RESOLVED', severity: 'WARNING', message: '未发送PLC指令' },
      { operationId: 'operation-2', code: 'SCAN_REPEATED', severity: 'WARNING', title: '扫码未计数', message: '两小时内已扫描，本次未增加次数' }
    ])

    expect(feedback.title).toBe('扫码未计数')
    expect(feedback.secondaryMessage).toBe('未发送PLC指令')
  })
})
