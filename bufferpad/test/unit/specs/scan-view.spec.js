import {
  buildUsageMetrics,
  currentCushionText,
  hasCurrentCushion
} from '@/modules/running/models/scan-view'

describe('running scan view helpers', () => {
  it('treats idle and failed scan values as no current cushion', () => {
    expect(hasCurrentCushion('')).toBe(false)
    expect(hasCurrentCushion('-')).toBe(false)
    expect(hasCurrentCushion('NoRead')).toBe(false)
    expect(currentCushionText('-')).toBe('等待扫码')
    expect(currentCushionText('NoRead')).toBe('扫码失败')
  })

  it('keeps a real cushion code and calculates its usage', () => {
    expect(hasCurrentCushion(' T8320260625047 ')).toBe(true)
    expect(currentCushionText('T8320260625047')).toBe('T8320260625047')
    expect(buildUsageMetrics('T8320260625047', 4, 550)).toEqual({
      hasCurrent: true,
      usedCount: 4,
      maxUseCount: 550,
      remainingCount: 546,
      percentage: 1
    })
  })

  it('never reports negative remaining usage or progress over 100 percent', () => {
    expect(buildUsageMetrics('PAD-1', 12, 10)).toEqual({
      hasCurrent: true,
      usedCount: 12,
      maxUseCount: 10,
      remainingCount: 0,
      percentage: 100
    })
  })

  it('uses null display values when no cushion is active', () => {
    expect(buildUsageMetrics('NoRead', 9, 100)).toEqual({
      hasCurrent: false,
      usedCount: null,
      maxUseCount: null,
      remainingCount: null,
      percentage: 0
    })
  })
})
