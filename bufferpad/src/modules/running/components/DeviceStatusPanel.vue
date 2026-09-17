<template>
  <div class="device-status-panel">
    <div v-if="sortedDevices.length === 0" class="empty-state">
      <i class="el-icon-info"></i>
      <span>当前产线未配置{{ title }}</span>
    </div>
    <article v-for="item in sortedDevices" :key="item.id" class="device-row">
      <span class="status-dot" :class="stateClass(item)"></span>
      <div class="device-copy">
        <div class="device-heading">
          <strong>{{ item.name || '未命名设备' }}</strong>
          <el-tag v-if="item.typeName" size="mini" effect="plain">{{ item.typeName }}</el-tag>
          <el-tag size="mini" effect="plain">{{ transportText(item) }}</el-tag>
          <el-tag size="mini" :type="tagType(item)">{{ stateText(item) }}</el-tag>
        </div>
        <div class="device-meta">
          <span><i class="el-icon-location-outline"></i>{{ positionText(item) }}</span>
          <span><i class="el-icon-connection"></i>{{ hostText(item) }}</span>
          <span v-if="item.statusChangedAt"><i class="el-icon-time"></i>状态更新 {{ formatTime(item.statusChangedAt) }}</span>
          <span v-if="item.lastCommunicationAt"><i class="el-icon-refresh"></i>最近成功 {{ formatTime(item.lastCommunicationAt) }}</span>
        </div>
        <div class="device-meta">
          <span>检测方式：{{ monitoringText(item) }}</span>
          <span v-if="item.lastRequestAt">最近请求 {{ requestText(item) }} · {{ formatTime(item.lastRequestAt) }}</span>
          <span v-if="item.consecutiveTimeouts">连续超时 {{ item.consecutiveTimeouts }} 次</span>
        </div>
        <p class="device-reason" :class="stateClass(item)">
          {{ item.statusReason || fallbackReason(item) }}
          <span v-if="Number(item.lastErrorCode) !== 0 && item.lastErrorCode !== null && item.lastErrorCode !== undefined">
            （错误码 {{ item.lastErrorCode }}）
          </span>
        </p>
      </div>
    </article>
  </div>
</template>

<script>
import dayjs from 'dayjs'
import {
  deviceFallbackReason,
  deviceTransportState,
  deviceStateCode,
  deviceStateLabel,
  sortDevicesByHealth
} from '../models/device-health'

export default {
  name: 'DeviceStatusPanel',
  props: {
    title: {
      type: String,
      required: true
    },
    devices: {
      type: Array,
      default: () => []
    }
  },
  computed: {
    sortedDevices() {
      return sortDevicesByHealth(this.devices)
    }
  },
  methods: {
    transportText(item) {
      return ({ CONNECTED: 'TCP已连接', DISCONNECTED: 'TCP未连接', CONNECTING: 'TCP连接中', UNKNOWN: 'TCP状态未知' })[deviceTransportState(item)] || 'TCP状态未知'
    },
    monitoringText(item) {
      return ({ PASSIVE: '被动接收，不因静默报警', SCANNER_HEARTBEAT: '已确认的扫码器心跳', PLC_HEARTBEAT: '已配置的PLC心跳写入', READ_PROBE: '已配置的PLC只读检测' })[item.monitoringMode] || '未配置主动检测'
    },
    requestText(item) {
      return ({ PLC_READ_PROBE: '只读检测', PLC_HEARTBEAT_WRITE: '心跳写入', PLC_BUSINESS_WRITE: '业务写入', PLC_BUSINESS_READ: '业务回读', SCANNER_HEARTBEAT: '扫码器心跳' })[item.lastRequestKind] || ''
    },
    stateCode(item) {
      return deviceStateCode(item)
    },
    stateClass(item) {
      return 'is-' + this.stateCode(item).toLowerCase()
    },
    stateText(item) {
      return deviceStateLabel(item)
    },
    fallbackReason(item) {
      return deviceFallbackReason(item)
    },
    tagType(item) {
      const types = {
        ONLINE: 'success',
        CONNECTING: '',
        DEGRADED: 'warning',
        OFFLINE: 'danger', TIMEOUT: 'danger', RETRYING: 'warning'
      }
      return types[this.stateCode(item)] || 'info'
    },
    positionText(item) {
      return item.installPositionName || item.position || '未设置安装位置'
    },
    hostText(item) {
      if (!item.ip) return '未设置网络地址'
      return item.ip + (item.port !== null && item.port !== undefined ? ':' + item.port : '')
    },
    formatTime(value) {
      const parsed = dayjs(value)
      return parsed.isValid() ? parsed.format('MM-DD HH:mm:ss') : value
    }
  }
}
</script>

<style scoped lang="less">
.device-status-panel { min-height: 200px; }
.device-row { display: flex; gap: 12px; padding: 16px 4px; border-bottom: 1px solid #ebeef5; }
.status-dot { flex: 0 0 11px; width: 11px; height: 11px; margin-top: 6px; border-radius: 50%; background: #909399; }
.status-dot.is-online { background: #67c23a; }
.status-dot.is-connecting { background: #409eff; }
.status-dot.is-degraded { background: #e6a23c; }
.status-dot.is-timeout, .status-dot.is-offline { background: #f56c6c; }
.device-copy { min-width: 0; flex: 1; }
.device-heading { display: flex; align-items: center; gap: 10px; }
.device-heading strong { min-width: 0; overflow-wrap: anywhere; }
.device-meta { display: flex; flex-wrap: wrap; gap: 8px 18px; margin-top: 8px; color: #909399; font-size: 12px; }
.device-meta i { margin-right: 5px; }
.device-reason { margin: 9px 0 0; padding: 7px 10px; border-radius: 4px; background: #f4f4f5; color: #606266; line-height: 1.5; overflow-wrap: anywhere; }
.device-reason.is-online { background: #f0f9eb; color: #529b2e; }
.device-reason.is-connecting { background: #ecf5ff; color: #337ecc; }
.device-reason.is-degraded { background: #fdf6ec; color: #b88230; }
.device-reason.is-timeout, .device-reason.is-offline { background: #fef0f0; color: #c45656; }
.empty-state { min-height: 180px; display: flex; align-items: center; justify-content: center; gap: 8px; color: #909399; }
</style>
