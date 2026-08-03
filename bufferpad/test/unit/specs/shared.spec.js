import { normalizeApiBaseUrl } from '@/shared/config/backend'
import { pageRows, pageTotal, responseMessage } from '@/shared/request/request'
import { formatScannerPosition, scannerPositionClass } from '@/shared/utils/format'

describe('shared config and helpers', () => {
  it('normalizes backend base url', () => {
    expect(normalizeApiBaseUrl('localhost:9001/api')).toBe('http://localhost:9001')
    expect(normalizeApiBaseUrl('/api')).toBe('/api')
  })

  it('reads page data safely', () => {
    const response = {
      status: 200,
      data: {
        codeSuccess: true,
        data: {
          totalPage: 2,
          data: [{ id: 1 }]
        }
      }
    }

    expect(pageRows(response)).toEqual([{ id: 1 }])
    expect(pageTotal(response)).toBe(2)

    response.data.data = { total: 3, data: [] }
    expect(pageTotal(response)).toBe(3)
  })

  it('formats scanner positions with legacy fallback', () => {
    expect(formatScannerPosition(1)).toBe('上')
    expect(formatScannerPosition(6, '新位置')).toBe('新位置')
    expect(scannerPositionClass(2)).toBe('error')
  })

  it('falls back to default response message', () => {
    expect(responseMessage(null, '失败')).toBe('失败')
  })
})
