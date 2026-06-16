import { buildBackendUrl } from '../shared/request/request'
import {
  exportCushions,
  getCushionByQrCode,
  getCushionDetails,
  getCushionPage,
  manualScan,
  updateCushionMaxUseCount
} from '../modules/cushion/api'
import { getDeviceConnectionStatus } from '../modules/device/api'
import { getScanLogs } from '../modules/log/api'
import {
  getDeviceTypeOptions,
  getInstallPositionOptions,
  getPLCAddrTypeOptions,
  getPLCDeviceOptions,
  getScannerDeviceOptions
} from '../modules/options/api'
import { getOpcConfigPage, resetOpcConfig, saveOpcConfig } from '../modules/settings/api/opc-config.api'
import { deleteDevice, getDevicePage, saveDevice } from '../modules/settings/api/device-info.api'
import { deletePLCAddr, getPLCAddrPage, savePLCAddr } from '../modules/settings/api/plc-addr.api'
import {
  deleteInstallPosition,
  getInstallPositionPage,
  saveInstallPosition
} from '../modules/settings/api/install-position.api'

export const getPLCreadCodeStatus = getDeviceConnectionStatus

export const getSseDevicesStatusUrl = id => {
  return buildBackendUrl('/sse/devicesStatus/' + `${id}`)
}

export const qrCodeGetData = getCushionByQrCode

export const postInfo = manualScan

export const getPageInfo = getCushionPage

export const changeMaxCount = updateCushionMaxUseCount

export const exportData = exportCushions

export const getDetails = getCushionDetails

export const getLogs = getScanLogs

export {
  deleteDevice,
  deleteInstallPosition,
  deletePLCAddr,
  getDevicePage,
  getDeviceTypeOptions,
  getInstallPositionOptions,
  getInstallPositionPage,
  getOpcConfigPage,
  getPLCAddrPage,
  getPLCAddrTypeOptions,
  getPLCDeviceOptions,
  getScannerDeviceOptions,
  resetOpcConfig,
  saveDevice,
  saveInstallPosition,
  saveOpcConfig,
  savePLCAddr
}
