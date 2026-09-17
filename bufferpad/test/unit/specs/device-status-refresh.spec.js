import Running from '../../../src/views/Layout/Running.vue'
import { loadDeviceStatus } from '../../../src/modules/running/services/running.service'
import { unknownDeviceStatuses, summarizeDevices, deviceStateLabel } from '../../../src/modules/running/models/device-health'
jest.mock('../../../src/modules/running/services/running.service', () => ({ loadDeviceStatus: jest.fn() }))

const response = devices => ({ status: 200, data: { codeSuccess: true, data: devices } })
const online = { id: 1, status: 1, statusCode: 'ONLINE' }
function state() {
  const vm = { ProdLine: 1, deviceStatusRequest: 0, devicesMessage: [online],
    showPageMessage: jest.fn(), handleRequestError: jest.fn(), handleOperationEvent: jest.fn(),
    lastSseErrorAt: Date.now() }
  Object.keys(Running.methods).forEach(key => { if (!vm[key]) vm[key] = Running.methods[key].bind(vm) })
  return vm
}
describe('authoritative device status refresh', () => {
  beforeEach(() => loadDeviceStatus.mockReset())
  it('invalidates stale online status on SSE disconnection', () => {
    const vm = state(); vm.handleSseDisconnected()
    expect(vm.devicesMessage[0].statusCode).toBe('UNKNOWN')
    expect(summarizeDevices(vm.devicesMessage).online).toBe(0)
  })
  it('reloads the full snapshot whenever SSE opens', async () => {
    const vm = state(); loadDeviceStatus.mockResolvedValue(response([{ id: 2, statusCode: 'OFFLINE' }]))
    vm.handleSseOpened(); await new Promise(resolve => setTimeout(resolve, 0))
    expect(loadDeviceStatus).toHaveBeenCalledWith(1)
    expect(vm.devicesMessage.map(d => d.id)).toEqual([2])
  })
  it('a late request cannot overwrite a newer response', async () => {
    const vm = state(); let finishOld
    loadDeviceStatus.mockImplementationOnce(() => new Promise(resolve => { finishOld = resolve }))
    loadDeviceStatus.mockImplementationOnce(() => Promise.resolve(response([{ id: 1, statusCode: 'OFFLINE' }])))
    const old = vm.refreshDeviceStatus(); await vm.refreshDeviceStatus()
    finishOld(response([online])); await old
    expect(vm.devicesMessage[0].statusCode).toBe('OFFLINE')
  })
  it('an unavailable snapshot never retains green online status', async () => {
    const vm = state(); loadDeviceStatus.mockRejectedValue(new Error('network'))
    await vm.refreshDeviceStatus()
    expect(vm.devicesMessage[0].statusCode).toBe('UNKNOWN')
    expect(vm.deviceStatusLoading).toBe(false)
  })
  it('disconnect invalidates already in-flight responses', async () => {
    const vm = state(); let finish
    loadDeviceStatus.mockImplementation(() => new Promise(resolve => { finish = resolve }))
    const pending = vm.refreshDeviceStatus(); vm.handleSseDisconnected()
    finish(response([online])); await pending
    expect(vm.devicesMessage[0].statusCode).toBe('UNKNOWN')
  })
  it('pending verification and unknown are never healthy', () => {
    expect(deviceStateLabel({ statusCode: 'VERIFYING' })).toBe('通信未验证')
    expect(summarizeDevices([{ statusCode: 'VERIFYING', status: 1 }]).state).toBe('UNVERIFIED')
    expect(summarizeDevices(unknownDeviceStatuses([online])).state).toBe('WARNING')
  })
})
