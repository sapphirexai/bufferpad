<template>
  <div class="page">
    <div ref="lineRef">
      <div style="display: flex; justify-content: space-between; align-items: center; padding: 0 100px;">
        <div style=" display: flex; align-items: center;">
          <p class="title" style="width: 20%;">产线:</p>
          <el-select
            v-model="ProdLine"
            placeholder="请选择"
            style="flex: 1;"
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
        <DeviceStatusPanel title="PLC状态" :devices="plcDevices" />
        <DeviceStatusPanel title="读码器状态" :devices="scannerDevices" />
      </div>
      <el-divider></el-divider>
    </div>
    <div ref="cardRef">
      <el-row style="display: flex; align-items: center; justify-content:space-between;">
        <el-col :span="4">
          <el-card>
            <p class="title" style="text-align: center; margin-bottom: 16px;">缓冲垫编号</p>
            <el-form style="width: 100%;" :model="ruleForm" label-position="right" :rules="rules" ref="ruleForm">
              <el-form-item prop="qrCode">
                <el-input
                  v-model="ruleForm.qrCode"
                  size="small"
                  ref="inputQrCode"
                  placeholder="接收数据中..."
                  :disabled="!handle"
                  name="qrCode"
                  @keydown.enter.native="handleEnterKey($event)"
                ></el-input>
              </el-form-item>
            </el-form>
            <el-button
              type="success"
              style="width: 100%"
              size="small"
              @click="addItem(handle)"
              >{{ handle ? "确定" : "手动输入" }}</el-button
            >
          </el-card>
        </el-col>
        <el-col :span="4" style="height: 100%;">
          <el-card class="card" style="flex-direction:column;height:100%;">
            <p class="title">当前缓冲垫</p>
            <div class="cunrrentId">
              <span>{{ currentQrCode }}</span>
            </div>
            <div class="cunrrentPosition">
              <span :class="showCodeClass(currentScannerSeq, currentScannerPosition)">
                {{ showCodeName(currentScannerSeq, currentScannerPosition) }}
              </span>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4" style="height: 100%;">
          <el-card class="card" style="flex-direction:column;height:100%;">
            <p class="title">使用次数</p>
            <div class="count" style="font-size: 56px; text-align: center;">
              <span :class="RemainCount === 0 ? 'error': (RemainCount > 5 ? 'success': 'warning')">{{ useCount }}</span>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4" style="height: 100%;">
          <el-card class="card" style="flex-direction:column;height: 100%;">
            <p class="title">剩余次数</p>
            <div class="count" style="font-size: 56px; text-align: center;">
              <span :class="RemainCount === 0 ? 'error': (RemainCount > 5 ? 'success': 'warning')">{{RemainCount}}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-divider></el-divider>
    </div>
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
      :data="tableData"
      style="width: 100%"
      :row-class-name="tableRowClassName"
      v-loading="loading"
      empty-text="暂无数据"
      border
      height="600"
      element-loading-text="数据玩命加载中"
      :cell-style="rowStyle"
      :cell-class-name="isCheckCell"
      :header-cell-style="rowStyle"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column prop="qrCode" label="缓冲垫编号" width="180"></el-table-column>
      <el-table-column prop="scannerSeq" label="缓冲垫位置" width="180">
        <template slot-scope="scope">
          <span :class="showCodeClass(scope.row.scannerSeq, scope.row.scannerPosition)">
            {{ showCodeName(scope.row.scannerSeq, scope.row.scannerPosition) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="openCount" label="开口数" width="180"></el-table-column>
      <el-table-column prop="createdDate" label="第一次使用时间" :formatter="formatDate"></el-table-column>
      <el-table-column prop="lastScanDate" label="最后一次使用时间" :formatter="formatDate"></el-table-column>
      <el-table-column prop="usedCount" label="当前使用次数">
        <template slot-scope="scope">
          <span :class="showColor(scope.row)">{{ scope.row.usedCount }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="maxUseCount" width="260px" label="使用寿命">
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
      <el-table-column label="操作">
        <template slot-scope="scope">
          <el-button class="button" type="text" size="mini" @click="getDetails(scope.row)" >明细</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
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
import DeviceStatusPanel from '../../modules/running/components/DeviceStatusPanel.vue';
import {
  exportRunningCushions,
  loadDeviceStatus,
  loadRunningCushions,
  saveCushionLife,
  submitManualScan
} from '../../modules/running/services/running.service';
import { createRunningSse } from '../../modules/running/services/running-sse.service';
import {
  isSuccessResponse,
  pageRows,
  pageTotal,
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
  components: { DeviceStatusPanel },
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
      devicesMessage: [{type: 0, status: 0}, {type: 1, status: 0}, {type: 2, status: 0}],
      loading: true,
      pageSizes: [8, 15, 20, 100, 10000],
      pageSize: 8,
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
      thresholdSettingVisible: false
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
        return 'warning-row';
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
        });
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
            const devicesMessage = this.devicesMessage
            devicesMessage.forEach((e, index) => {
              if (res.data.data.id === e.id) {
                devicesMessage[index] = res.data.data
              }
            })

            this.devicesMessage = [...devicesMessage]
          }
        },
        error => {
          if (error && error.message) this.handleRequestError(error)
        }
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
      return this.Count - this.useCount
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
  height: 100%;
  display: flex;
  flex-direction: column;
}
.el-table .warning-row {
  background: rgb(255, 0, 0);
}

.el-table .success-row {
  background: #f0f9eb;
}
.card {
  display: flex;
  justify-content: space-between;
  width: 100%;
  align-items: center;

  .cunrrentId {
    font-size: 18px;
    margin: 26.5px 0;
    text-align: center;
  }

  .cunrrentPosition {
    font-size: 24px;
    text-align: center;
  }
}

.table-action {
  display: flex;
  justify-content: space-between;
  padding-bottom: 12px;

  .search-input {
    display: flex;
    align-items: center;
    & .text {
      width: 140px;
      font-size: 14px;
      font-weight: normal;
      color: #606266;
    }
  }
}

.card .title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  text-align: center;
}
.title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.card .code {
  font-size: 16px;
  font-weight: bold;
  margin-top: 16px;
}

.card .count {
  font-weight: bold;
  color: #67C23A;
  margin-top: 33px;

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

.connection-status {
  max-height: 120px;
  margin-left: 16px;
  padding: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  .info {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 4px;
    &:last-child {
      margin-bottom: 0px;
    }

    .name {
      display: inline-block;
      margin-right: 16px;
    }
  }
}

.isCheckCell {
  padding: 7.5px 0 !important;
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
    margin-left: 24px;
    height: 28px;
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
</style>
