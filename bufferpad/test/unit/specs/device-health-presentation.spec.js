import Vue from 'vue'
import Overview from '../../../src/modules/running/components/DeviceHealthOverview.vue'
import Panel from '../../../src/modules/running/components/DeviceStatusPanel.vue'
import { summarizeDevices, deviceStateCode, unknownDeviceStatuses } from '../../../src/modules/running/models/device-health'

describe('transport and communication health presentation', () => {
  it('does not count passive, unverified, checking or unknown devices as faults', () => {
    const list=['VERIFYING','ONLINE','CONNECTING','RETRYING','UNKNOWN'].map((statusCode,id)=>({id,statusCode}))
    const s=summarizeDevices(list);expect(s.attention).toBe(0);expect(s.unverified).toBe(1);expect(s.unknown).toBe(1)
  })
  it('counts only confirmed faults and preserves timeout across reconnection', () => {
    const list=['OFFLINE','DEGRADED','TIMEOUT'].map(statusCode=>({statusCode}))
    expect(summarizeDevices(list).attention).toBe(3)
    expect(deviceStateCode({statusCode:'CONNECTING',communicationState:'TIMEOUT'})).toBe('TIMEOUT')
  })
  it('shows zero faults for TCP-only devices without claiming all verified', () => {
    const vm=new (Vue.extend(Overview))({propsData:{plcDevices:[{statusCode:'VERIFYING',status:1}],scannerDevices:[{statusCode:'VERIFYING',status:1}],loading:false}})
    expect(vm.systemSummary.attention).toBe(0)
    expect(vm.systemSummaryText).toContain('未验证')
    expect(vm.drawerSummaryText).toContain('故障 0 台')
    expect(vm.groupSummaryText(vm.scannerSummary)).toBe('1/1 TCP已连接');vm.$destroy()
  })
  it('does not carry a previous server fault into a missing-status snapshot', () => {
    const result=unknownDeviceStatuses([{statusCode:'TIMEOUT',communicationState:'TIMEOUT',transportState:'CONNECTED'}])
    expect(deviceStateCode(result[0])).toBe('UNKNOWN');expect(summarizeDevices(result).attention).toBe(0)
  })
  it('presents TCP and business verification as different labels', () => {
    const vm=new (Vue.extend(Panel))({propsData:{title:'扫码器',devices:[]}})
    const device={statusCode:'VERIFYING',transportState:'CONNECTED',monitoringMode:'PASSIVE'}
    expect(vm.transportText(device)).toBe('TCP已连接');expect(vm.stateText(device)).toBe('通信未验证')
    expect(vm.monitoringText(device)).toContain('不因静默报警');vm.$destroy()
  })
})
