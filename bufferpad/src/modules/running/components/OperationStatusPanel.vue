<template>
  <section class="operation-status" :class="panelClass">
    <div class="operation-result">
      <span class="result-icon" aria-hidden="true"><i :class="iconClass"></i></span>
      <el-tag v-if="currentEvent" size="mini" :type="tagType">{{ severityText }}</el-tag>
      <strong class="result-title">{{ currentEvent ? currentEvent.title : '等待扫码' }}</strong>
      <span class="result-message">
        {{ currentEvent ? currentEvent.message : '系统已就绪，等待读码器或手动输入' }}
      </span>
      <time v-if="currentEvent">{{ formatTime(currentEvent.occurredAt) }}</time>
    </div>

    <div class="operation-actions">
      <button
        type="button"
        class="attention-count"
        :class="{ 'has-attention': attentionEvents.length > 0 }"
        @click="drawerVisible = true"
      >
        <i class="el-icon-bell"></i>
        <span>运行告警</span>
        <strong>{{ attentionEvents.length }}条</strong>
      </button>
      <el-button type="text" size="small" icon="el-icon-document" @click="drawerVisible = true">
        查看详情
      </el-button>
    </div>

    <el-drawer
      title="运行告警与最近操作"
      :visible.sync="drawerVisible"
      size="560px"
      append-to-body
      custom-class="operation-event-drawer"
    >
      <div class="event-list">
        <div v-for="item in events" :key="item.eventId || item.id" class="event-row">
          <span class="event-dot" :class="severityClass(item.severity)"></span>
          <div class="event-body">
            <div class="event-row-title">
              <strong>{{ item.title }}</strong>
              <time>{{ formatTime(item.occurredAt) }}</time>
            </div>
            <p>{{ item.message }}</p>
            <p v-if="item.suggestion" class="event-advice">处理建议：{{ item.suggestion }}</p>
            <div class="event-technical">
              <span v-if="item.qrCode">缓冲垫：{{ item.qrCode }}</span>
              <span v-if="item.deviceName">设备：{{ item.deviceName }}</span>
              <span v-if="item.address">地址：{{ item.address }}</span>
              <span v-if="item.errorCode !== null && item.errorCode !== undefined">错误码：{{ item.errorCode }}</span>
            </div>
          </div>
        </div>
        <el-empty v-if="events.length === 0" description="暂无运行记录"></el-empty>
      </div>
    </el-drawer>
  </section>
</template>

<script>
import dayjs from 'dayjs'
import {
  isAttentionEvent,
  normalizeSeverity,
  severityElementType,
  severityLabel
} from '../models/operation-event'

export default {
  name: 'OperationStatusPanel',
  props: {
    currentEvent: {
      type: Object,
      default: null
    },
    events: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      drawerVisible: false
    }
  },
  computed: {
    normalizedSeverity() {
      return normalizeSeverity(this.currentEvent && this.currentEvent.severity)
    },
    panelClass() {
      return 'is-' + this.normalizedSeverity.toLowerCase()
    },
    tagType() {
      return severityElementType(this.normalizedSeverity)
    },
    severityText() {
      return severityLabel(this.normalizedSeverity)
    },
    iconClass() {
      if (this.normalizedSeverity === 'ERROR') return 'el-icon-circle-close'
      if (this.normalizedSeverity === 'WARNING') return 'el-icon-warning-outline'
      return this.currentEvent ? 'el-icon-circle-check' : 'el-icon-time'
    },
    attentionEvents() {
      return this.events.filter(isAttentionEvent)
    }
  },
  methods: {
    formatTime(value) {
      if (!value) return ''
      const parsed = dayjs(value)
      return parsed.isValid() ? parsed.format('MM-DD HH:mm:ss') : value
    },
    severityClass(severity) {
      return 'is-' + normalizeSeverity(severity).toLowerCase()
    }
  }
}
</script>

<style scoped lang="less">
.operation-status {
  min-height: 54px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  border: 1px solid #d9ecff;
  border-left: 4px solid #67c23a;
  border-radius: 4px;
  background: #f4fbf1;
  overflow: hidden;
}

.operation-status.is-warning {
  border-color: #f5dab1;
  border-left-color: #e6a23c;
  background: #fdf8ec;
}

.operation-status.is-error {
  border-color: #fbc4c4;
  border-left-color: #f56c6c;
  background: #fef0f0;
}

.operation-result {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 14px;
  white-space: nowrap;
}

.result-icon {
  flex: 0 0 auto;
  color: #67c23a;
  font-size: 21px;
  line-height: 1;
}

.is-warning .result-icon { color: #e6a23c; }
.is-error .result-icon { color: #f56c6c; }

.result-title {
  flex: 0 0 auto;
  max-width: 220px;
  overflow: hidden;
  color: #303133;
  font-size: 15px;
  text-overflow: ellipsis;
}

.result-message {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  color: #606266;
  text-overflow: ellipsis;
}

.operation-result time {
  flex: 0 0 auto;
  color: #909399;
  font-size: 12px;
}

.operation-actions {
  height: 32px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 14px;
  border-left: 1px solid rgba(144, 147, 153, 0.2);
}

.attention-count {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #67c23a;
  cursor: pointer;
  white-space: nowrap;
}

.attention-count.has-attention { color: #c45656; }
.attention-count strong { font-size: 14px; }

.event-list { padding: 0 22px 24px; }
.event-row { display: flex; gap: 12px; padding: 14px 0; border-bottom: 1px solid #ebeef5; }
.event-dot { flex: 0 0 10px; height: 10px; margin-top: 6px; border-radius: 50%; background: #67c23a; }
.event-dot.is-warning { background: #e6a23c; }
.event-dot.is-error { background: #f56c6c; }
.event-body { min-width: 0; flex: 1; }
.event-row-title { display: flex; justify-content: space-between; gap: 12px; }
.event-row-title strong { overflow-wrap: anywhere; }
.event-row-title time { flex: 0 0 auto; color: #909399; font-size: 12px; }
.event-body p { margin: 6px 0 0; line-height: 1.5; overflow-wrap: anywhere; }
.event-advice { color: #606266; }
.event-technical { display: flex; flex-wrap: wrap; gap: 6px 12px; margin-top: 7px; color: #909399; font-size: 12px; }

@media (max-width: 900px) {
  .operation-status { grid-template-columns: minmax(0, 1fr); }
  .operation-actions { justify-content: flex-end; border-top: 1px solid rgba(144, 147, 153, 0.2); border-left: 0; }
  .result-title { max-width: 150px; }
  .operation-result time { display: none; }
}

@media (max-width: 600px) {
  .result-message { display: none; }
  .operation-actions { justify-content: space-between; }
}

/deep/ .operation-event-drawer { max-width: 92vw; }
</style>
