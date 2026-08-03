<template>
  <section class="operation-status" :class="panelClass">
    <div class="operation-result">
      <span class="result-icon" aria-hidden="true"><i :class="iconClass"></i></span>
      <el-tag v-if="currentEvent" size="mini" :type="tagType">{{ severityText }}</el-tag>
      <strong class="result-title">{{ currentEvent ? currentEvent.title : '等待扫码' }}</strong>
      <span class="result-message">
        {{ currentEvent ? currentMessage : '系统已就绪，等待读码器或手动输入' }}
      </span>
      <time v-if="currentEvent">{{ formatTime(currentEvent.occurredAt) }}</time>
    </div>

    <div class="operation-actions">
      <button
        type="button"
        class="operation-action attention-count"
        :class="{ 'has-attention': attentionGroups.length > 0 }"
        @click="drawerVisible = true"
      >
        <span class="action-icon" aria-hidden="true"><i class="el-icon-bell"></i></span>
        <span>运行告警</span>
        <strong>{{ attentionGroups.length }}条</strong>
      </button>
      <button type="button" class="operation-action detail-action" @click="drawerVisible = true">
        <span class="action-icon" aria-hidden="true"><i class="el-icon-document"></i></span>
        <span>查看详情</span>
      </button>
    </div>

    <el-drawer
      title="运行告警与最近操作"
      :visible.sync="drawerVisible"
      size="560px"
      append-to-body
      custom-class="operation-event-drawer"
    >
      <div class="event-list">
        <article v-for="group in operationGroups" :key="group.operationId" class="event-row">
          <span class="event-dot" :class="severityClass(group.severity)"></span>
          <div class="event-body">
            <div class="event-row-title">
              <div>
                <strong>{{ group.title }}</strong>
                <el-tag v-if="group.eventCount > 1" size="mini" type="info">
                  本次操作 {{ group.eventCount }} 项结果
                </el-tag>
              </div>
              <time>{{ formatTime(group.occurredAt) }}</time>
            </div>
            <p>{{ group.message }}</p>
            <p v-if="group.secondaryMessage" class="event-secondary">{{ group.secondaryMessage }}</p>
            <p v-if="group.suggestion" class="event-advice">处理建议：{{ group.suggestion }}</p>
            <div class="event-technical">
              <span v-if="group.qrCode">缓冲垫：{{ group.qrCode }}</span>
              <span v-if="group.deviceName">设备：{{ group.deviceName }}</span>
              <span v-if="group.address">地址：{{ group.address }}</span>
              <span v-if="group.errorCode !== null && group.errorCode !== undefined">错误码：{{ group.errorCode }}</span>
            </div>
            <details v-if="group.eventCount > 1" class="event-details">
              <summary>查看本次操作完整过程</summary>
              <div v-for="item in group.events" :key="item.eventId || item.id" class="event-detail-row">
                <span :class="severityClass(item.severity)">{{ severityLabel(item.severity) }}</span>
                <div>
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.message }}</p>
                  <small v-if="item.technicalDetail">技术信息：{{ item.technicalDetail }}</small>
                </div>
              </div>
            </details>
          </div>
        </article>
        <el-empty v-if="operationGroups.length === 0" description="暂无运行记录"></el-empty>
      </div>
    </el-drawer>
  </section>
</template>

<script>
import dayjs from 'dayjs'
import {
  groupOperationEvents,
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
    currentMessage() {
      if (!this.currentEvent) return ''
      return this.currentEvent.secondaryMessage
        ? this.currentEvent.message + '；' + this.currentEvent.secondaryMessage
        : this.currentEvent.message
    },
    operationGroups() {
      return groupOperationEvents(this.events, 20)
    },
    attentionGroups() {
      return this.operationGroups.filter(isAttentionEvent)
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
    },
    severityLabel
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

.operation-action {
  height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #409eff;
  font: inherit;
  line-height: 20px;
  cursor: pointer;
  white-space: nowrap;
}

.action-icon {
  width: 16px;
  height: 20px;
  display: inline-flex;
  flex: 0 0 16px;
  align-items: center;
  justify-content: center;
  line-height: 20px;
}

.action-icon i { display: block; line-height: 20px; }
.attention-count { color: #67c23a; }
.attention-count.has-attention { color: #c45656; }
.attention-count strong { font-size: 14px; line-height: 20px; }

.event-list { padding: 0 22px 24px; }
.event-row { display: flex; gap: 12px; padding: 14px 0; border-bottom: 1px solid #ebeef5; }
.event-dot { flex: 0 0 10px; height: 10px; margin-top: 6px; border-radius: 50%; background: #67c23a; }
.event-dot.is-warning { background: #e6a23c; }
.event-dot.is-error { background: #f56c6c; }
.event-body { min-width: 0; flex: 1; }
.event-row-title { display: flex; justify-content: space-between; gap: 12px; }
.event-row-title > div { min-width: 0; display: flex; flex-wrap: wrap; align-items: center; gap: 7px; }
.event-row-title strong { overflow-wrap: anywhere; }
.event-row-title time { flex: 0 0 auto; color: #909399; font-size: 12px; }
.event-body p { margin: 6px 0 0; line-height: 1.5; overflow-wrap: anywhere; }
.event-secondary { color: #606266; }
.event-advice { color: #606266; }
.event-technical { display: flex; flex-wrap: wrap; gap: 6px 12px; margin-top: 7px; color: #909399; font-size: 12px; }
.event-details { margin-top: 9px; color: #606266; }
.event-details summary { color: #409eff; cursor: pointer; }
.event-detail-row { display: grid; grid-template-columns: 38px minmax(0, 1fr); gap: 8px; padding: 9px 0 0 8px; }
.event-detail-row > span { font-size: 12px; color: #67c23a; }
.event-detail-row > span.is-warning { color: #e6a23c; }
.event-detail-row > span.is-error { color: #f56c6c; }
.event-detail-row p { margin-top: 2px; }
.event-detail-row small { display: block; margin-top: 3px; color: #909399; overflow-wrap: anywhere; }

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
