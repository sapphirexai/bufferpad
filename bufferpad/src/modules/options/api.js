import { get } from '../../shared/request/request'

export function getDeviceTypeOptions() {
  return get('/options/deviceTypes')
}

export function getPLCAddrTypeOptions() {
  return get('/options/plcAddrTypes')
}

export function getInstallPositionOptions() {
  return get('/options/deviceInstallPositions')
}

export function getPLCDeviceOptions() {
  return get('/options/plcDevices')
}

export function getScannerDeviceOptions() {
  return get('/options/scannerDevices')
}

