export const SIEMENS_S7_1200_DEVICE_TYPE = 3
export const SIEMENS_S7_1500_DEVICE_TYPE = 4
export const SIEMENS_S7_DEFAULT_PORT = 102

export function isSiemensS7DeviceType(type) {
  const value = Number(type)
  return value === SIEMENS_S7_1200_DEVICE_TYPE || value === SIEMENS_S7_1500_DEVICE_TYPE
}

export function plcAddressPlaceholder(type) {
  if (isSiemensS7DeviceType(type)) return '例如 DB1.DBW0、MW0、IW0 或 QW0'
  return '请输入PLC寄存器地址，例如 D6600 或 MW10000'
}

export function plcAddressHint(type) {
  if (isSiemensS7DeviceType(type)) {
    return '西门子使用S7绝对地址，系统按16位整数读写。数据块请使用非优化访问，并在PLC中允许PUT/GET通信。'
  }
  return ''
}
