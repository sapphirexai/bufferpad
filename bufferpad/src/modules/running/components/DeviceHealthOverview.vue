<template>
  <section class="device-health-overview">
    <div class="health-summary">
      <div class="system-health" :class="summaryClass(systemSummary)">
        <span class="health-icon"><i class="el-icon-monitor"></i></span>
        <span class="summary-copy">
          <span class="summary-label">设备状态</span>
          <strong>{{ systemSummaryText }}</strong>
        </span>
      </div>

      <button type="button" class="device-summary" @click="openDrawer('plc')">
        <span class="status-dot" :class="summaryClass(plcSummary)"></span>
        <span class="summary-copy">
          <span class="summary-label">PLC</span>
          <strong>{{ groupSummaryText(plcSummary) }}</strong>
        </span>
        <span v-if="!loading && plcSummary.attention" class="group-attention">
          故障 {{ plcSummary.attention }}
        </span>
        <i class="el-icon-arrow-right"></i>
      </button>

      <button type="button" class="device-summary" @click="openDrawer('scanner')">
        <span class="status-dot" :class="summaryClass(scannerSummary)"></span>
        <span class="summary-copy">
          <span class="summary-label">读码器</span>
          <strong>{{ groupSummaryText(scannerSummary) }}</strong>
        </span>
        <span v-if="!loading && scannerSummary.attention" class="group-attention">
          故障 {{ scannerSummary.attention }}
        </span>
        <i class="el-icon-arrow-right"></i>
      </button>

      <el-button icon="el-icon-s-unfold" size="small" @click="openDrawer('plc')">
        查看设备
      </el-button>
    </div>

    <el-drawer
      title="设备连接状态"
      :visible.sync="drawerVisible"
      size="720px"
      append-to-body
      custom-class="device-status-drawer"
    >
      <div class="drawer-content">
        <div class="drawer-overview" :class="summaryClass(systemSummary)">
          <div>
            <span>当前产线设备</span>
            <strong>{{ systemSummaryText }}</strong>
          </div>
          <span>{{ drawerSummaryText }}</span>
        </div>
        <el-tabs v-model="activeTab">
          <el-tab-pane :label="'PLC（' + plcSummary.total + '）'" name="plc">
            <DeviceStatusPanel title="PLC" :devices="plcDevices" />
          </el-tab-pane>
          <el-tab-pane :label="'读码器（' + scannerSummary.total + '）'" name="scanner">
            <DeviceStatusPanel title="读码器" :devices="scannerDevices" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </section>
</template>

<script>
import DeviceStatusPanel from './DeviceStatusPanel.vue'
import { summarizeDevices, summarizeSystem } from '../models/device-health'

export default {
  name: 'DeviceHealthOverview',
  components: { DeviceStatusPanel },
  props: {
    plcDevices: {
      type: Array,
      default: () => []
    },
    scannerDevices: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      drawerVisible: false,
      activeTab: 'plc'
    }
  },
  computed: {
    plcSummary() {
      return summarizeDevices(this.plcDevices)
    },
    scannerSummary() {
      return summarizeDevices(this.scannerDevices)
    },
    systemSummary() {
      return summarizeSystem(this.plcDevices, this.scannerDevices)
    },
    systemSummaryText() {
      if (this.loading) return '状态加载中'
      if (this.systemSummary.state === 'UNCONFIGURED') return '未配置设备'
      if (this.systemSummary.attention) return '设备故障 ' + this.systemSummary.attention + ' 台'
      if (this.systemSummary.unknown) return '部分设备状态未知'
      if (this.systemSummary.connecting || this.systemSummary.retrying) return '设备连接或验证中'
      if (this.systemSummary.unverified) return 'TCP已连接，部分通信未验证'
      return '全部连接已验证'
    },
    drawerSummaryText() {
      if (this.loading) return '正在读取连接状态'
      if (this.systemSummary.total === 0) return '请先配置设备信息'
      const s = this.systemSummary
      return '共 ' + s.total + ' 台，故障 ' + s.attention + ' 台，未验证 ' + s.unverified + ' 台，状态未知 ' + s.unknown + ' 台'
    }
  },
  methods: {
    openDrawer(tab) {
      this.activeTab = tab
      this.drawerVisible = true
    },
    groupSummaryText(summary) {
      if (this.loading) return '加载中'
      if (summary.total === 0) return '未配置'
      return summary.connected + '/' + summary.total + ' TCP已连接'
    },
    summaryClass(summary) {
      return 'is-' + String(summary.state || 'UNCONFIGURED').toLowerCase()
    }
  }
}
</script>

<style scoped lang="less">
.device-health-overview {
  min-width: 0;
}

.health-summary {
  min-height: 50px;
  display: grid;
  grid-template-columns: minmax(150px, 0.75fr) minmax(180px, 1fr) minmax(180px, 1fr) auto;
  align-items: center;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
}

.system-health,
.device-summary {
  min-width: 0;
  min-height: 34px;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0 14px;
  border: 0;
  border-right: 1px solid #ebeef5;
  background: transparent;
  color: #303133;
  text-align: left;
}

.device-summary {
  cursor: pointer;
}

.device-summary:hover {
  background: #f5f7fa;
}

.device-summary .el-icon-arrow-right {
  flex: 0 0 auto;
  margin-left: 2px;
  color: #c0c4cc;
}

.health-summary > .el-button {
  margin: 0 12px;
}

.health-icon {
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f0f9eb;
  color: #67c23a;
  font-size: 16px;
}

.is-error .health-icon { background: #fef0f0; color: #f56c6c; }
.is-warning .health-icon { background: #fdf6ec; color: #e6a23c; }
.is-unconfigured .health-icon { background: #f4f4f5; color: #909399; }

.summary-copy {
  min-width: 0;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.summary-label {
  color: #909399;
  font-size: 12px;
  line-height: 16px;
}

.summary-copy strong {
  overflow: hidden;
  font-size: 14px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-dot {
  width: 9px;
  height: 9px;
  flex: 0 0 9px;
  border-radius: 50%;
  background: #67c23a;
}

.status-dot.is-error { background: #f56c6c; }
.status-dot.is-warning { background: #e6a23c; }
.status-dot.is-unconfigured { background: #909399; }

.group-attention {
  flex: 0 0 auto;
  color: #c45656;
  font-size: 12px;
  white-space: nowrap;
}

.drawer-content { padding: 0 24px 24px; }
.drawer-overview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  padding: 14px 16px;
  border-left: 4px solid #67c23a;
  background: #f0f9eb;
}
.drawer-overview.is-error { border-left-color: #f56c6c; background: #fef0f0; }
.drawer-overview.is-warning { border-left-color: #e6a23c; background: #fdf6ec; }
.drawer-overview.is-unconfigured { border-left-color: #909399; background: #f4f4f5; }
.drawer-overview div { display: flex; flex-direction: column; gap: 4px; }
.drawer-overview span { color: #909399; font-size: 12px; }

@media (max-width: 1100px) {
  .health-summary {
    grid-template-columns: minmax(140px, 0.7fr) minmax(160px, 1fr) minmax(160px, 1fr) auto;
  }
  .system-health,
  .device-summary { padding: 0 10px; }
  .group-attention { display: none; }
}

@media (max-width: 760px) {
  .health-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    row-gap: 6px;
    padding: 6px;
  }
  .system-health,
  .device-summary { border-right: 0; }
  .health-summary > .el-button { justify-self: start; margin: 0 10px; }
}

@media (max-width: 520px) {
  .health-summary { grid-template-columns: minmax(0, 1fr); }
  .drawer-overview { align-items: flex-start; flex-direction: column; }
}

/deep/ .device-status-drawer {
  max-width: 92vw;
}
</style>

<style scoped>
.status-dot.is-unverified { background: #909399; }
.is-unverified .health-icon { background: #f4f4f5; color: #909399; }
.drawer-overview.is-unverified { background: #f4f4f5; border-left-color: #909399; }
</style>
