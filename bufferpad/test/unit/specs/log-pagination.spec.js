import Vue from 'vue'
import Logs from '../../../src/views/Layout/logs.vue'
import { getScanLogs } from '../../../src/modules/log/api'
jest.mock('../../../src/modules/log/api', () => ({ getScanLogs: jest.fn() }))

function response(rows, total) {
  return { status: 200, data: { codeSuccess: true, data: { data: rows, totalPage: total } } }
}

describe('log server pagination', () => {
  let vm
  beforeEach(() => {
    getScanLogs.mockReset()
    getScanLogs.mockImplementation(() => Promise.resolve(response([], 0)))
    vm = new (Vue.extend(Logs))()
    vm.$message = { error: jest.fn() }
  })
  afterEach(() => vm.$destroy())

  it('requests 20 rows by default and displays server totals', async () => {
    getScanLogs.mockResolvedValueOnce(response([{ id: 42 }], 1234))
    await vm.refreshLogList()
    expect(getScanLogs.mock.calls[0][0]).toMatchObject({ currentPage: 1, pageSize: 20 })
    expect(vm.tableData).toEqual([{ id: 42 }])
    expect(vm.total).toBe(1234)
  })
  it('changes pages and preserves filters', () => {
    vm.searchFormData.qrCode = 'pad-42'
    vm.searchFormData.status = 'FAILED'; vm.searchFormData.scanner = 'ID=1'; vm.searchFormData.plc = '位置5'
    vm.handleCurrentChange(3)
    expect(getScanLogs.mock.calls[0][0]).toMatchObject({ currentPage: 3, pageSize: 20, qrCode: 'pad-42', status: 'FAILED', scanner: 'ID=1', plc: '位置5' })
  })
  it('returns to page one when changing page size or searching', async () => {
    vm.currentPage = 5
    vm.handleSizeChange(50)
    expect(getScanLogs.mock.calls[0][0]).toMatchObject({ currentPage: 1, pageSize: 50 })
    vm.currentPage = 7
    vm.searchFormData.timeValue = null
    await vm.searchList()
    expect(getScanLogs.mock.calls[1][0]).toMatchObject({ currentPage: 1, pageSize: 50, startTime: '', endTime: '' })
  })
  it('clears old rows when a filter has no matches', async () => {
    vm.tableData = [{ id: 1 }]; vm.total = 100
    await vm.searchList()
    expect(vm.tableData).toEqual([])
    expect(vm.total).toBe(0)
  })
  it('does not display an older request over a newer page', async () => {
    let resolveOld
    getScanLogs.mockImplementationOnce(() => new Promise(resolve => { resolveOld = resolve }))
    const old = vm.refreshLogList()
    getScanLogs.mockResolvedValueOnce(response([{ id: 2 }], 1))
    await vm.searchList()
    resolveOld(response([{ id: 1 }], 999))
    await old
    expect(vm.tableData).toEqual([{ id: 2 }])
    expect(vm.total).toBe(1)
    expect(vm.loading).toBe(false)
  })
  it('reports errors and clears stale results', async () => {
    vm.tableData = [{ id: 1 }]; vm.total = 5
    getScanLogs.mockRejectedValueOnce(new Error('查询失败'))
    await vm.searchList()
    expect(vm.$message.error).toHaveBeenCalledWith(expect.stringContaining('查询失败'))
    expect(vm.total).toBe(0)
    expect(vm.loading).toBe(false)
  })
  it('labels new result states and preserves legacy rows', () => {
    expect(vm.resultLabel('PROCESSING')).toBe('处理中')
    expect(vm.resultLabel('UNKNOWN')).toBe('结果未知')
    expect(vm.resultLabel(null)).toBe('历史日志')
  })
})
