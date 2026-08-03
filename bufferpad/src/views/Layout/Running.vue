<template>
  <div class="page">
    <div ref="lineRef" class="monitor-header">
      <div class="line-status-bar">
        <div class="line-selector">
          <span class="title">产线</span>
          <el-select
            v-model="ProdLine"
            placeholder="请选择"
            :popper-append-to-body="false"
          >
            <el-option
              v-for="item in options"
              :key="item.value"
              :label="item.label + '号产线'"
              :value="item.value"
              :disabled="item.disabled"
            >
            </el-option>
          </el-select>
        </div>
        <DeviceHealthOverview
          :plc-devices="plcDevices"
          :scanner-devices="scannerDevices"
          :loading="deviceStatusLoading"
        />
      </div>
    </div>
    <OperationStatusPanel
      :current-event="currentOperationEvent"
      :events="operationEvents"
    />
    <section class="scan-workbench" :class="usageStateClass">
      <div class="scan-control">
        <div class="workbench-label">
          <i class="el-icon-full-screen"></i>
          <span>扫码输入</span>
        </div>
        <div class="scan-form-row">
          <el-form
            ref="ruleForm"
            class="scan-form"
            :model="ruleForm"
            :rules="rules"
          >
            <el-form-item prop="qrCode">
              <el-input
                ref="inputQrCode"
                v-model="ruleForm.qrCode"
                size="small"
                :placeholder="handle ? '请输入缓冲垫编号' : '等待读码器扫码'"
                :disabled="!handle"
                name="qrCode"
                @keydown.enter.native="handleEnterKey($event)"
              ></el-input>
            </el-form-item>
          </el-form>
          <el-button type="primary" size="small" @click="addItem(handle)">
            {{ handle ? '确定' : '手动输入' }}
          </el-button>
        </div>
      </div>

      <div class="current-cushion">
        <span class="workbench-label">当前缓冲垫</span>
        <strong :title="currentCushionDisplay">{{ currentCushionDisplay }}</strong>
        <span
          v-if="hasCurrentCushionData"
          class="position"
          :class="showCodeClass(currentScannerSeq, currentScannerPosition)"
        >
          {{ showCodeName(currentScannerSeq, currentScannerPosition) }}
        </span>
        <span v-else class="position is-muted">尚无缓冲垫数据</span>
      </div>

      <div class="usage-summary">
        <div class="usage-values">
          <div class="usage-metric">
            <span>已用 / 寿命</span>
            <strong>{{ displayUsedCount }} <small>/ {{ displayMaxCount }}</small></strong>
          </div>
          <div class="usage-divider"></div>
          <div class="usage-metric remaining-metric">
            <span>剩余次数</span>
            <strong>{{ displayRemainCount }}</strong>
          </div>
        </div>
        <el-progress
          :percentage="currentUsageMetrics.percentage"
          :show-text="false"
          :stroke-width="6"
          :color="usageProgressColor"
        ></el-progress>
      </div>
    </section>
    <div ref="actionRef" class="table-action">
      <div class="search-input">
        <span class="text">搜索缓冲垫：</span>
        <el-input
          size="small"
          placeholder="请输入缓冲垫编号"
          v-model="searchQrCode"
          @keydown.enter.native="handleSearchQrCode"
        >
          <i @click="handleSearchQrCode" slot="suffix" style="cursor: pointer;" class="el-input__icon el-icon-search"></i>
        </el-input>
      </div>
      <div v-if="!thresholdSettingVisible">
        <span>{{ '阈值: ' + warningThresholdPer*100 + '%' }}</span>
        <i @click="thresholdSettingVisible = true" class="el-icon-edit"></i>
      </div>
      <div v-if="thresholdSettingVisible">
        <el-input-number v-model="warningThresholdPer" :min="0" :max="1" :step="0.05" size="small" placeholder="请输入阈值百分比0%~100%"/>
        <el-button class="button" type="primary" size="mini" @click="saveToLocalStorage" >确定</el-button>
      </div>
      <div class="table-button">
        <el-button @click="goLogs" size="small" type="primary">日志查询</el-button>
        <el-button @click="openDialog" size="small" type="primary">批量修改</el-button>
        <el-button icon="el-icon-upload2" size="small" @click="exportExcel">导出</el-button>
      </div>
    </div>
    <el-table
      class="cushion-table"
      :data="tableData"
      style="width: 100%"
      size="small"
      :row-class-name="tableRowClassName"
      v-loading="loading"
      empty-text="暂无数据"
      border
      element-loading-text="数据玩命加载中"
      :cell-style="rowStyle"
      :cell-class-name="isCheckCell"
      :header-cell-style="rowStyle"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column prop="qrCode" label="缓冲垫编号" width="170" show-overflow-tooltip></el-table-column>
      <el-table-column prop="scannerSeq" label="缓冲垫位置" width="130">
        <template slot-scope="scope">
          <span :class="showCodeClass(scope.row.scannerSeq, scope.row.scannerPosition)">
            {{ showCodeName(scope.row.scannerSeq, scope.row.scannerPosition) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="openCount" label="开口数" width="80"></el-table-column>
      <el-table-column prop="createdDate" label="第一次使用时间" min-width="150" show-overflow-tooltip :formatter="formatDate"></el-table-column>
      <el-table-column prop="lastScanDate" label="最后一次使用时间" min-width="150" show-overflow-tooltip :formatter="formatDate"></el-table-column>
      <el-table-column prop="usedCount" label="当前使用次数" width="110">
        <template slot-scope="scope">
          <span :class="showColor(scope.row)">{{ scope.row.usedCount }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="maxUseCount" width="170" label="使用寿命">
        <template slot-scope="scope">
          <div class="update-maxUseCount" v-if="!scope.row.isCheck">
            <span>{{ scope.row.maxUseCount }}</span>
            <i @click="updateMaxUseCount(scope.row)" class="el-icon-edit"></i>
          </div>
          <div class="isCheck-maxUseCount" v-else>
            <el-input-number v-model="scope.row.maxUseCount" size="small" label="请输入"></el-input-number>
            <el-button class="button" type="primary" size="mini" @click="enterChangeMaxCount(scope.row)" >确定</el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template slot-scope="scope">
          <el-button class="button" type="text" size="mini" @click="getDetails(scope.row)" >明细</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="cushion-pagination"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page.sync="currentPage"
      :page-sizes="pageSizes"
      :page-size.sync="pageSize"
      :pager-count="5"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
    >
    </el-pagination>
    <el-dialog
      title="修改使用寿命"
      :visible.sync="dialogVisible"
      width="500px"
      @close="closeDialog"
    >
      <el-form :model="dialogRuleForm" label-width="150px" label-position="right" :rules="dialogRules" ref="dialogRuleFormRef">
        <el-form-item prop="maxUseCount" label="使用寿命">
          <el-input-number
            v-model="dialogRuleForm.maxUseCount"
            label="请输入使用寿命"
          ></el-input-number>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="closeDialog">取 消</el-button>
        <el-button type="primary" @click="enterBatchChangeMaxCount">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>
<script>
import DeviceHealthOverview from '../../modules/running/components/DeviceHealthOverview.vue';
import OperationStatusPanel from '../../modules/running/components/OperationStatusPanel.vue';
import {
  exportRunningCushions,
  loadDeviceStatus,
  loadRunningCushions,
  saveCushionLife,
  submitManualScan
} from '../../modules/running/services/running.service';
import { createRunningSse } from '../../modules/running/services/running-sse.service';
import { loadRecentOperationEvents } from '../../modules/running/services/operation-event.service';
import { normalizeSeverity, prependOperationEvent } from '../../modules/running/models/operation-event';
import {
  buildUsageMetrics,
  currentCushionText,
  hasCurrentCushion
} from '../../modules/running/models/scan-view';
import {
  isSuccessResponse,
  pageRows,
  pageTotal,
  responseData,
  requestErrorMessage,
  responseMessage
} from '../../shared/request/request';
import { getFilenameFromDisposition, downloadBlob } from '../../shared/utils/download';
import {
  centerCellStyle,
  formatScannerPosition,
  scannerPositionClass,
  tableDateFormatter
} from '../../shared/utils/format';
export default {
  name: 'Running',
  components: { DeviceHealthOverview, OperationStatusPanel },
  data() {
    return {
      handle: false,
      options: [
        {
          value: '1',
          label: '1'
        },
        {
          value: '2',
          label: '2'
        },
        {
          value: '3',
          label: '3'
        },
        {
          value: '4',
          label: '4'
        },
        {
          value: '5',
          label: '5'
        }
      ],
      ProdLine: '1',
      status: 'success',
      tableData: [],
      input: '',
      events: null,
      useCount: 0,
      devicesMessage: [],
      loading: true,
      pageSizes: [10, 15, 20, 100, 10000],
      pageSize: 10,
      currentPage: 1,
      total: 0,
      Count: 0,
      ruleForm: {
        qrCode: ''
      },
      dialogRuleForm: {
        maxUseCount: ''
      },
      rules: {
        qrCode: [{ required: true, message: '请输入二维码', trigger: 'blur' }]
      },
      dialogRules: {
        maxUseCount: [{ required: true, message: '请输入使用寿命', trigger: 'blur' }]
      },
      searchQrCode: '',
      currentQrCode: '-',
      currentScannerSeq: '',
      currentScannerPosition: '',
      dialogVisible: false,
      multipleSelection: [],
      warningThresholdPer: 0.95,
      thresholdSettingVisible: false,
      operationEvents: [],
      currentOperationEvent: null,
      deviceStatusLoading: true,
      sseConnected: false,
      sseOpenedOnce: false,
      lastSseErrorAt: 0
    };
  },
  created() {
    // 从本地存储加载数据
    this.loadFromLocalStorage();
  },
  methods: {
    handleRequestError(error) {
      this.$message.error(requestErrorMessage(error))
    },
    buildPageParams() {
      return {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        qrCode: this.searchQrCode
      }
    },
    setRunningTable(res) {
      const result = pageRows(res)
      this.tableData = result.filter(item => {
        return item.workLine === Number(this.ProdLine)
      }).map(item => {
        return {
          ...item,
          isCheck: false
        }
      })
      this.total = pageTotal(res)
    },
    openDialog() {
      if (this.multipleSelection.length === 0) {
        this.$message.warning('请选择修改数据')
        return
      }
      this.dialogVisible = true
    },
    goLogs() {
      this.$router.push('/logs')
    },
    handleClose() {
      this.dialogVisible = false
    },
    handleSelectionChange(val) {
      this.multipleSelection = val
    },
    showCodeName(value, position) {
      return formatScannerPosition(value, position)
    },
    showCodeClass(value, position) {
      return scannerPositionClass(value, position)
    },
    showColor(row) {
      const colorName = row.usedCount >= row.maxUseCount ? 'info' : (row.usedCount < row.maxUseCount * this.warningThresholdPer ? 'success' : 'warning')
      return colorName
    },
    updateMaxUseCount(row) {
      row.isCheck = true
    },
    isCheckCell({row, column, rowIndex, columnIndex}) {
      if (row.isCheck && column.property === 'maxUseCount') {
        return 'isCheckCell'
      } else {
        return ''
      }
    },
    tableRowClassName({ row, rowIndex }) {
      if (row.usedCount >= row.maxUseCount) {
        return 'running-expired-row';
      } else {
        return '';
      }
    },

    handleSearchQrCode() {
      this.refreshCushionList()
    },

    formatDate(row, column, cellValue, index) {
      return tableDateFormatter(row, column, cellValue, index);
    },
    addItem(flag) {
      if (flag) {
        // 调用添加数据到数据库的api
        this.$refs['ruleForm'].validate(valid => {
          if (valid) {
            submitManualScan(this.ProdLine, this.ruleForm.qrCode.trim()).then(res => {
              if (isSuccessResponse(res)) {
                this.Count = res.data.data.maxUseCount
                this.useCount = res.data.data.usedCount
                this.currentQrCode = res.data.data.qrCode
                this.currentScannerSeq = res.data.data.scannerSeq
                this.currentScannerPosition = res.data.data.scannerPosition
                this.$message.success('扫码成功')
                // this.handle = false;

                this.currentPage = 1
                this.searchQrCode = ''

                this.refreshCushionList();
              } else {
                this.$message.error(responseMessage(res))
              }
            }).catch(error => {
              this.handleRequestError(error)
            }).finally(() => {
              this.ruleForm.qrCode = ''
              this.$refs.inputQrCode.focus()
            });
          }
        });
      } else {
        this.handle = true;
      }
    },
    refreshDeviceStatus() {
      this.deviceStatusLoading = true
      loadDeviceStatus(this.ProdLine)
        .then(res => {
          if (isSuccessResponse(res)) {
            this.devicesMessage = res.data.data || []
          } else {
            this.$message.error(responseMessage(res))
          }
        })
        .catch(error => {
          this.handleRequestError(error)
        })
        .finally(() => {
          this.deviceStatusLoading = false
        });
    },
    refreshOperationEvents() {
      loadRecentOperationEvents(Number(this.ProdLine), 20)
        .then(res => {
          const events = responseData(res, [])
          this.operationEvents = Array.isArray(events) ? events : []
          this.currentOperationEvent = this.operationEvents.length > 0 ? this.operationEvents[0] : null
        })
        .catch(error => {
          this.handleRequestError(error)
        })
    },
    handleOperationEvent(event, showNotice) {
      if (!event) return
      this.operationEvents = prependOperationEvent(this.operationEvents, event, 20)
      this.currentOperationEvent = event
      if (!showNotice) return
      const severity = normalizeSeverity(event.severity)
      if (severity === 'ERROR') this.$message.error(event.title + '：' + event.message)
      if (severity === 'WARNING') this.$message.warning(event.title + '：' + event.message)
    },
    handleSseDisconnected() {
      this.sseConnected = false
      const now = Date.now()
      if (now - this.lastSseErrorAt < 10000) return
      this.lastSseErrorAt = now
      this.handleOperationEvent({
        eventId: 'sse-disconnected-' + this.ProdLine + '-' + now,
        code: 'SSE_DISCONNECTED',
        severity: 'WARNING',
        title: '实时消息连接中断',
        message: '页面正在自动重新连接，期间可手动刷新数据',
        suggestion: '长时间未恢复时，请检查后端服务和网络',
        workLine: Number(this.ProdLine),
        occurredAt: new Date().toISOString()
      }, false)
    },
    handleSseOpened() {
      const wasDisconnected = this.sseOpenedOnce && !this.sseConnected
      this.sseConnected = true
      this.sseOpenedOnce = true
      if (wasDisconnected && this.currentOperationEvent && this.currentOperationEvent.code === 'SSE_DISCONNECTED') {
        this.handleOperationEvent({
          eventId: 'sse-connected-' + this.ProdLine + '-' + Date.now(),
          code: 'SSE_CONNECTED',
          severity: 'INFO',
          title: '实时消息连接已恢复',
          message: '运行数据将继续自动更新',
          workLine: Number(this.ProdLine),
          occurredAt: new Date().toISOString()
        }, false)
      }
    },
    handleSizeChange(val) {
      this.pageSize = val
      this.currentPage = 1
      this.refreshCushionList()
    },
    handleCurrentChange(val) {
      this.currentPage = val
      this.refreshCushionList()
    },
    refreshCushionList() {
      this.loading = true
      loadRunningCushions(this.buildPageParams())
        .then(res => {
          if (isSuccessResponse(res)) {
            this.setRunningTable(res)
          } else {
            this.$message.error(responseMessage(res))
          }
        })
        .catch(error => {
          this.handleRequestError(error)
        })
        .finally(() => {
          this.loading = false
        });
    },
    refreshRunningSse() {
      // 目前的做法是和后端做的单向长链接，这里的接口就不放在 API 列表中处理，直接在这里作为参数传入
      if (this.events) {
        this.events.close()
        this.events = null
      }
      this.events = createRunningSse(
        this.ProdLine,
        res => {
          if (!res || !res.data) return
          if (res.data.topic === 'cushionInfo') {
            if (res.data.data !== null) {
              // this.ruleForm.qrCode = res.data.data.qrCode
              this.currentQrCode = res.data.data.qrCode
              this.currentScannerSeq = res.data.data.scannerSeq
              this.currentScannerPosition = res.data.data.scannerPosition
              this.useCount = res.data.data.usedCount
              this.Count = res.data.data.maxUseCount

              this.currentPage = 1
              this.searchQrCode = ''
              this.dialogVisible = false

              this.refreshCushionList();
              if (res.codeSuccess) {
                this.$message.success(res.msg)
              } else {
                this.$message.error(res.msg)
              }
            } else {
              this.currentQrCode = 'NoRead';
              this.$message.error(res.msg)
            }
          }
          if (res.data.topic === 'deviceStatus') {
            const devicesMessage = [...this.devicesMessage]
            const deviceIndex = devicesMessage.findIndex(item => item.id === res.data.data.id)
            if (deviceIndex >= 0) devicesMessage.splice(deviceIndex, 1, res.data.data)
            else devicesMessage.push(res.data.data)
            this.devicesMessage = devicesMessage
          }
          if (res.data.topic === 'operationEvent') {
            this.handleOperationEvent(res.data.data, true)
          }
        },
        () => {
          this.handleSseDisconnected()
        },
        () => this.handleSseOpened()
      );
    },
    rowStyle() {
      return centerCellStyle();
    },
    handleEnterKey(event) {
      event.preventDefault();
    },
    async changeMaxUsedCount(data, row) {
      try {
        const params = {...data}
        const res = await saveCushionLife(params)
        if (isSuccessResponse(res)) {
          this.$message.success('修改完成')
          if (!row) {
            this.dialogVisible = false
          } else {
            row.isCheck = false
          }
          this.refreshCushionList()
        } else {
          this.$message.error(responseMessage(res))
        }
      } catch (error) {
        this.handleRequestError(error)
      }
    },
    enterChangeMaxCount(row) {
      if (!row.maxUseCount) {
        this.$message.error('请填入有效数字')
      } else if (row.maxUseCount < 0) {
        this.$message.error('使用寿命不能小于 0')
      } else {
        const params = {
          maxUseCount: row.maxUseCount,
          ids: [row.id]
        }

        this.changeMaxUsedCount(params, row)
      }
    },
    enterBatchChangeMaxCount() {
      this.$refs['dialogRuleFormRef'].validate(valid => {
        if (valid) {
          if (this.dialogRuleForm.maxUseCount < 0) {
            this.$message.error('使用寿命不能小于 0')
            return
          }
          const ids = this.multipleSelection.map(item => item.id)
          const params = {
            maxUseCount: this.dialogRuleForm.maxUseCount,
            ids
          }

          this.changeMaxUsedCount(params)
        }
      })
    },
    closeDialog() {
      this.$refs['dialogRuleFormRef'].resetFields()
      this.dialogVisible = false
    },
    closeThresholdSetting() {
      this.thresholdSettingVisible = false
    },
    async exportExcel() {
      if (this.multipleSelection.length === 0) {
        this.$message.warning('请选择导出数据')
        return false
      }

      const ids = this.multipleSelection.map(item => item.id)
      try {
        const res = await exportRunningCushions(ids)

        if (res.status === 200) {
          const fileName = getFilenameFromDisposition(res.headers['content-disposition'], '缓冲垫数据.xlsx')
          downloadBlob(res.data, fileName)
        } else {
          this.$message.error('导出失败')
        }
      } catch (error) {
        this.handleRequestError(error)
      }
    },
    getDetails(row) {
      this.$router.push({
        path: '/details',
        query: {
          qrCode: row.qrCode
        }
      })
    },
    // 保存数据到本地存储
    saveToLocalStorage() {
      if (this.warningThresholdPer < 0 || this.warningThresholdPer > 1) {
        this.$message.error('请输入0~1之间的数字')
        return
      }
      const data = {
        warningThresholdPer: this.warningThresholdPer
      };
      localStorage.setItem('bufferPadData', JSON.stringify(data));
      this.thresholdSettingVisible = false;
      this.$message.success('预警阈值变更为' + this.warningThresholdPer * 100 + '%');
    },
    // 从本地存储加载数据
    loadFromLocalStorage() {
      const savedData = localStorage.getItem('bufferPadData');
      if (savedData) {
        try {
          const data = JSON.parse(savedData);
          const warningThresholdPer = Number(data.warningThresholdPer);
          if (warningThresholdPer >= 0 && warningThresholdPer <= 1) {
            this.warningThresholdPer = warningThresholdPer;
          }
        } catch (error) {
          localStorage.removeItem('bufferPadData');
        }
      }
    }
  },
  watch: {
    ProdLine: {
      handler(newval, oldval) {
        this.refreshDeviceStatus();
        this.refreshCushionList();
        this.refreshOperationEvents();
        this.sseConnected = false
        this.sseOpenedOnce = false
        this.refreshRunningSse();
      },
      immediate: true
    },
    currentQrCode(newVal, oldVal) {
      if (newVal === 'NoRead') {
        this.$message.warning('扫码失败，请手动输入');
      }
    },
    RemainCount(newval, oldval) {
      if (newval === 0) {
        this.$message.error('扫码次数达上限')
      }
    }
  },
  computed: {
    RemainCount() {
      return Math.max(0, this.Count - this.useCount)
    },
    hasCurrentCushionData() {
      return hasCurrentCushion(this.currentQrCode)
    },
    currentCushionDisplay() {
      return currentCushionText(this.currentQrCode)
    },
    currentUsageMetrics() {
      return buildUsageMetrics(this.currentQrCode, this.useCount, this.Count)
    },
    displayUsedCount() {
      return this.currentUsageMetrics.hasCurrent ? this.currentUsageMetrics.usedCount : '--'
    },
    displayMaxCount() {
      return this.currentUsageMetrics.hasCurrent ? this.currentUsageMetrics.maxUseCount : '--'
    },
    displayRemainCount() {
      return this.currentUsageMetrics.hasCurrent ? this.currentUsageMetrics.remainingCount : '--'
    },
    usageStateClass() {
      if (!this.currentUsageMetrics.hasCurrent) return 'is-idle'
      if (this.currentUsageMetrics.remainingCount === 0) return 'is-expired'
      return this.currentUsageMetrics.percentage >= this.warningThresholdPer * 100
        ? 'is-warning'
        : 'is-normal'
    },
    usageProgressColor() {
      if (!this.currentUsageMetrics.hasCurrent) return '#dcdfe6'
      if (this.currentUsageMetrics.remainingCount === 0) return '#f56c6c'
      if (this.currentUsageMetrics.percentage >= this.warningThresholdPer * 100) return '#e6a23c'
      return '#67c23a'
    },
    plcDevices() {
      return this.devicesMessage.filter(item => item.type !== 0)
    },
    scannerDevices() {
      return this.devicesMessage.filter(item => item.type === 0)
    }
  },
  beforeDestroy() {
    if (this.events) {
      this.events.close();
    }
  }
};
</script>
<style lang="less">
.page {
  min-height: 100%;
  padding: 4px 6px 24px;
}

.monitor-header {
  margin-bottom: 8px;
}

.line-status-bar {
  display: grid;
  grid-template-columns: minmax(200px, 250px) minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.line-selector {
  display: flex;
  align-items: center;
  gap: 10px;

  .title {
    flex: 0 0 auto;
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  .el-select {
    min-width: 0;
    flex: 1;
  }
}

.operation-status {
  margin-bottom: 8px;
}

.scan-workbench {
  min-height: 94px;
  display: grid;
  grid-template-columns: minmax(300px, 1.1fr) minmax(220px, 0.9fr) minmax(300px, 1fr);
  align-items: stretch;
  margin-bottom: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fff;
}

.scan-control,
.current-cushion,
.usage-summary {
  min-width: 0;
  padding: 13px 16px;
}

.scan-control,
.current-cushion {
  border-right: 1px solid #ebeef5;
}

.workbench-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 13px;
  line-height: 18px;
}

.workbench-label i {
  color: #409eff;
}

.scan-form-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  margin-top: 9px;
}

.scan-form {
  min-width: 0;
  flex: 1;
}

.scan-form .el-form-item {
  margin-bottom: 0;
}

.scan-form .el-form-item__content {
  line-height: 32px;
}

.scan-form .el-input,
.scan-form .el-input__inner,
.scan-form-row > .el-button {
  height: 32px;
}

.scan-form .el-input {
  display: block;
}

.scan-form-row > .el-button {
  min-width: 80px;
  margin: 1px 0 0;
}

.current-cushion {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  grid-template-rows: auto 1fr auto;
  align-items: start;
}

.current-cushion strong {
  min-width: 0;
  overflow: hidden;
  color: #303133;
  font-size: 17px;
  line-height: 24px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.current-cushion .position {
  overflow: hidden;
  font-size: 13px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.current-cushion .position.is-muted {
  color: #909399;
  font-size: 12px;
}

.usage-summary {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.usage-values {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 1px minmax(0, 0.8fr);
  align-items: center;
  margin-bottom: 10px;
}

.usage-divider {
  width: 1px;
  height: 34px;
  background: #ebeef5;
}

.usage-metric {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 10px;
}

.usage-metric > span {
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
}

.usage-metric strong {
  color: #67c23a;
  font-size: 24px;
  line-height: 28px;
  white-space: nowrap;
}

.usage-metric strong small {
  color: #606266;
  font-size: 14px;
  font-weight: 500;
}

.scan-workbench.is-idle .usage-metric strong {
  color: #909399;
}

.scan-workbench.is-warning .usage-metric strong {
  color: #e6a23c;
}

.scan-workbench.is-expired .usage-metric strong {
  color: #f56c6c;
}

.el-table .running-expired-row > td {
  background: #fff1f1;
  color: #9f1d2b;
}

.el-table .running-expired-row:hover > td {
  background: #ffe4e6 !important;
}

.el-table .success-row {
  background: #f0f9eb;
}
.table-action {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 4;
  margin: 0 -6px;
  padding: 7px 6px 8px;
  background: #fff;

  .search-input {
    display: flex;
    align-items: center;
    & .text {
      width: 118px;
      font-size: 14px;
      font-weight: normal;
      color: #606266;
    }
  }
}
.title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.cushion-table {
  min-height: 248px;
}

.cushion-table /deep/ th,
.cushion-table /deep/ td {
  padding: 6px 0;
}

.cushion-table /deep/ .cell {
  line-height: 20px;
  white-space: nowrap;
}

.cushion-pagination {
  padding: 8px 0 2px;
  text-align: left;
}
.error {
  color: #F56C6C;
}

.success {
  color: #67C23A;
}

.warning {
  color: #E6A23C;
}

.info {
  color: #909399;
}

.isCheckCell {
  padding: 4px 0 !important;
}

.update-maxUseCount i{
  display: inline-block;
  margin-left: 4px;
  cursor: pointer;
}

.isCheck-maxUseCount {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;

  .button {
    margin-left: 8px;
    height: 28px;
  }

  .el-input-number {
    width: 90px;
  }
}

/deep/ el-input .el-input__inner {
  background-color: rgba(255, 255, 255, 0.247);
}

.blink {
  animation: blink 0.5s infinite steps(1);
}
@keyframes blink {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0;
  }
}

@media (max-width: 1200px) {
  .line-status-bar {
    grid-template-columns: minmax(0, 1fr);
    gap: 8px;
  }

  .line-selector {
    max-width: 240px;
  }

  .scan-workbench {
    grid-template-columns: minmax(260px, 1fr) minmax(200px, 0.8fr) minmax(280px, 1fr);
  }
}

@media (max-width: 960px) {

  .line-selector {
    max-width: 260px;
  }

  .scan-workbench {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .usage-summary {
    grid-column: 1 / -1;
    border-top: 1px solid #ebeef5;
  }

  .current-cushion {
    border-right: 0;
  }

  .table-action {
    flex-wrap: wrap;

    .search-input {
      min-width: 260px;
      flex: 1;
    }
  }
}

@media (max-width: 600px) {
  .line-status-bar,
  .scan-workbench {
    grid-template-columns: minmax(0, 1fr);
  }

  .line-selector {
    grid-column: auto;
  }

  .page {
    padding-right: 0;
    padding-left: 0;
  }

  .scan-control,
  .current-cushion {
    border-right: 0;
    border-bottom: 1px solid #ebeef5;
  }

  .usage-summary {
    grid-column: auto;
    border-top: 0;
  }

  .table-button {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .table-button .el-button {
    margin-left: 0;
  }
}
</style>
