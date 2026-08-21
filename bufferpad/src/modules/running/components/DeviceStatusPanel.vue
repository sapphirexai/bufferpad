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
          <el-tag size="mini" :type="tagType(item)">{{ stateText(item) }}</el-tag>
        </div>
        <div class="device-meta">
          <span><i class="el-icon-location-outline"></i>{{ positionText(item) }}</span>
          <span><i class="el-icon-connection"></i>{{ hostText(item) }}</span>
          <span v-if="item.statusChangedAt"><i class="el-icon-time"></i>状态更新 {{ formatTime(item.statusChangedAt) }}</span>
          <span v-if="item.lastCommunicationAt"><i class="el-icon-refresh"></i>最近通信 {{ formatTime(item.lastCommunicationAt) }}</span>
        </div>
        <p class="device-reason" :class="stateClass(item)">
          {{ item.statusReason || fallbackReason(item) }}
          <span v-if="item.lastErrorCode !== null && item.lastErrorCode !== undefined">
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
        OFFLINE: 'danger'
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
.status-dot.is-offline { background: #f56c6c; }
.device-copy { min-width: 0; flex: 1; }
.device-heading { display: flex; align-items: center; gap: 10px; }
.device-heading strong { min-width: 0; overflow-wrap: anywhere; }
.device-meta { display: flex; flex-wrap: wrap; gap: 8px 18px; margin-top: 8px; color: #909399; font-size: 12px; }
.device-meta i { margin-right: 5px; }
.device-reason { margin: 9px 0 0; padding: 7px 10px; border-radius: 4px; background: #f4f4f5; color: #606266; line-height: 1.5; overflow-wrap: anywhere; }
.device-reason.is-online { background: #f0f9eb; color: #529b2e; }
.device-reason.is-connecting { background: #ecf5ff; color: #337ecc; }
.device-reason.is-degraded { background: #fdf6ec; color: #b88230; }
.device-reason.is-offline { background: #fef0f0; color: #c45656; }
.empty-state { min-height: 180px; display: flex; align-items: center; justify-content: center; gap: 8px; color: #909399; }
</style>
