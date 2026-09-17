import { createOperationFeedbackCenter } from '@/modules/running/services/operation-feedback-center'

describe('operation feedback center', () => {
  beforeEach(() => {
    jest.useFakeTimers()
  })

  afterEach(() => {
    jest.clearAllTimers()
  })

  it('shows only one notification for all events in the same operation', () => {
    const feedbacks = []
    const center = createOperationFeedbackCenter({
      delay: 300,
      onFeedback: (feedback, showNotice) => {
        if (showNotice) feedbacks.push(feedback)
      }
    })

    center.ingest({
      eventId: 'scan-event',
      operationId: 'operation-1',
      code: 'SCAN_REPEATED',
      severity: 'WARNING',
      title: '扫码未计数',
      message: '两小时内已扫描，本次未增加次数'
    }, true)
    center.ingest({
      eventId: 'plc-event',
      operationId: 'operation-1',
      code: 'PLC_TARGET_NOT_RESOLVED',
      severity: 'WARNING',
      title: 'PLC目标无法确定',
      message: '系统没有发送PLC指令'
    }, true)
    center.ingest({
      eventId: 'plc-event',
      operationId: 'operation-1',
      code: 'PLC_TARGET_NOT_RESOLVED',
      severity: 'WARNING'
    }, true)

    jest.runAllTimers()

    expect(feedbacks).toHaveLength(1)
    expect(feedbacks[0].eventCount).toBe(2)
    expect(feedbacks[0].title).toBe('扫码未计数')
    center.destroy()
  })

  it('does not notify when seeding historical events', () => {
    const onFeedback = jest.fn()
    const center = createOperationFeedbackCenter({ delay: 300, onFeedback })

    center.seed([{ eventId: 'history-1', operationId: 'old-operation', severity: 'ERROR' }])
    jest.runAllTimers()

    expect(onFeedback.mock.calls.filter(call => call[1] === true)).toHaveLength(0)
    center.destroy()
  })

  it('waits for a terminal PLC event and includes it in the only notification', () => {
    const feedbacks = []
    const center = createOperationFeedbackCenter({
      delay: 1500,
      terminalDelay: 180,
      onFeedback: (feedback, showNotice) => {
        if (showNotice) feedbacks.push(feedback)
      }
    })

    center.ingest({
      eventId: 'scan-event',
      operationId: 'operation-2',
      code: 'SCAN_COUNTED',
      severity: 'INFO',
      message: 'scan counted'
    }, true)
    jest.advanceTimersByTime(800)
    expect(feedbacks).toHaveLength(0)

    center.ingest({
      eventId: 'plc-event',
      operationId: 'operation-2',
      code: 'PLC_WRITE_FAILED',
      severity: 'ERROR',
      message: 'PLC write failed'
    }, true)
    jest.advanceTimersByTime(180)

    expect(feedbacks).toHaveLength(1)
    expect(feedbacks[0].eventCount).toBe(2)
    expect(feedbacks[0].code).toBe('PLC_WRITE_FAILED')
    center.destroy()
  })
})
