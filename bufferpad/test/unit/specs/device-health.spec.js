import {
  deviceStateCode,
  sortDevicesByHealth,
  summarizeDevices,
  summarizeSystem
} from '@/modules/running/models/device-health'

describe('device health helpers', () => {
  it('supports the detailed state code and the legacy numeric status', () => {
    expect(deviceStateCode({ statusCode: 'degraded', status: 1 })).toBe('DEGRADED')
    expect(deviceStateCode({ status: 1 })).toBe('ONLINE')
    expect(deviceStateCode({ status: 0 })).toBe('OFFLINE')
  })

  it('summarizes configured, online and attention device counts', () => {
    const summary = summarizeDevices([
      { statusCode: 'ONLINE' },
      { statusCode: 'CONNECTING' },
      { statusCode: 'OFFLINE' }
    ])

    expect(summary.total).toBe(3)
    expect(summary.online).toBe(1)
    expect(summary.attention).toBe(2)
    expect(summary.state).toBe('ERROR')
    expect(summarizeDevices([]).state).toBe('UNCONFIGURED')
  })

  it('puts devices requiring attention before healthy devices', () => {
    const devices = sortDevicesByHealth([
      { name: '在线设备', statusCode: 'ONLINE' },
      { name: '连接中设备', statusCode: 'CONNECTING' },
      { name: '离线设备', statusCode: 'OFFLINE' }
    ])

    expect(devices.map(item => item.name)).toEqual(['离线设备', '连接中设备', '在线设备'])
  })

  it('summarizes PLC and scanner health as one system state', () => {
    expect(summarizeSystem(
      [{ statusCode: 'ONLINE' }],
      [{ statusCode: 'ONLINE' }]
    )).toEqual({ total: 2, attention: 0, state: 'HEALTHY' })

    expect(summarizeSystem(
      [{ statusCode: 'ONLINE' }],
      [{ statusCode: 'CONNECTING' }]
    ).state).toBe('WARNING')
  })
})
