import Vue from 'vue'
import Export from '../../../src/modules/running/components/TimeRangeExport.vue'
import { exportCushionsByTime } from '../../../src/modules/cushion/api'
import { downloadBlob } from '../../../src/shared/utils/download'
jest.mock('../../../src/modules/cushion/api', () => ({ exportCushionsByTime: jest.fn() }))
jest.mock('../../../src/shared/utils/download', () => ({ downloadBlob: jest.fn(), getFilenameFromDisposition: () => 'test.xlsx' }))
describe('time range cushion export', () => {
  let vm
  const success = { data: new Blob(['xlsx']), headers: { 'content-type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' } }
  beforeEach(() => {
    jest.clearAllMocks(); exportCushionsByTime.mockReset(); vm = new (Vue.extend(Export))()
    vm.$message = { warning: jest.fn(), error: jest.fn(), success: jest.fn() }
    exportCushionsByTime.mockResolvedValue(success)
  })
  afterEach(() => vm.$destroy())
  it('opens with last use and a complete day by default', () => {
    vm.timeType = 'FIRST_USE'; vm.open()
    expect(vm.visible).toBe(true); expect(vm.timeType).toBe('LAST_USE')
    expect(vm.range[0]).toMatch(/00:00:00$/); expect(vm.range[1]).toMatch(/23:59:59$/)
  })
  ;['FIRST_USE', 'LAST_USE'].forEach(type => it('sends the selected ' + type + ' field and downloads once', async () => {
    vm.open(); vm.timeType = type; await vm.submit()
    expect(exportCushionsByTime).toHaveBeenCalledWith({ timeType: type, startTime: vm.range[0], endTime: vm.range[1] })
    expect(downloadBlob).toHaveBeenCalledTimes(1); expect(vm.visible).toBe(false); expect(vm.exporting).toBe(false)
  }))
  it('rejects empty and reversed ranges', async () => {
    await vm.submit(); vm.range = ['2026-09-15 00:00:00', '2026-09-14 00:00:00']; await vm.submit()
    expect(exportCushionsByTime).not.toHaveBeenCalled(); expect(vm.$message.warning).toHaveBeenCalledTimes(2)
  })
  it('keeps the dialog and reports server JSON without downloading it', async () => {
    exportCushionsByTime.mockResolvedValueOnce({ data: new Blob([JSON.stringify({ msg: '所选时间段没有缓冲垫信息' })]), headers: { 'content-type': 'application/json' } })
    vm.open(); await vm.submit()
    expect(downloadBlob).not.toHaveBeenCalled(); expect(vm.visible).toBe(true)
    expect(vm.$message.error).toHaveBeenCalledWith(expect.stringContaining('所选时间段没有缓冲垫信息'))
  })
  it('reports HTTP failures and permits retry', async () => {
    exportCushionsByTime.mockRejectedValueOnce({ response: { data: new Blob([JSON.stringify({ msg: '请缩小时间范围' })]) } })
    vm.open(); await vm.submit(); expect(vm.$message.error).toHaveBeenCalledWith('请缩小时间范围')
    expect(vm.exporting).toBe(false); await vm.submit(); expect(downloadBlob).toHaveBeenCalledTimes(1)
  })
  it('prevents concurrent duplicate exports', async () => {
    let finish
    exportCushionsByTime.mockReset()
    exportCushionsByTime.mockImplementationOnce(() => new Promise(resolve => { finish = resolve }))
    vm.open(); const pending = vm.submit(); await vm.submit()
    expect(exportCushionsByTime).toHaveBeenCalledTimes(1); finish(success); await pending
  })
})
