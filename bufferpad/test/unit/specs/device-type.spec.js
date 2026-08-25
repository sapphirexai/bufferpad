import {
  isSiemensS7DeviceType,
  plcAddressHint,
  plcAddressPlaceholder,
  SIEMENS_S7_DEFAULT_PORT
} from '@/modules/settings/models/device-type'

describe('Siemens S7 device configuration', () => {
  it('recognizes the shared S7 type and the historical S7-1500 code', () => {
    expect(isSiemensS7DeviceType(3)).toBe(true)
    expect(isSiemensS7DeviceType('4')).toBe(true)
    expect(isSiemensS7DeviceType(1)).toBe(false)
    expect(SIEMENS_S7_DEFAULT_PORT).toBe(102)
  })

  it('shows S7 address guidance only for Siemens devices', () => {
    expect(plcAddressPlaceholder(3)).toContain('DB1.DBW0')
    expect(plcAddressHint(4)).toContain('PUT/GET')
    expect(plcAddressPlaceholder(1)).toContain('D6600')
    expect(plcAddressHint(1)).toBe('')
  })
})
