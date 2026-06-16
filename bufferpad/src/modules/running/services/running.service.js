import {
  exportCushions,
  getCushionPage,
  manualScan,
  updateCushionMaxUseCount
} from '../../cushion/api'
import { getDeviceConnectionStatus } from '../../device/api'

export function loadDeviceStatus(workLine) {
  return getDeviceConnectionStatus(workLine)
}

export function loadRunningCushions(params) {
  return getCushionPage(params)
}

export function submitManualScan(workLine, qrCode) {
  return manualScan(workLine, qrCode)
}

export function saveCushionLife(data) {
  return updateCushionMaxUseCount(data)
}

export function exportRunningCushions(ids) {
  return exportCushions(ids)
}

