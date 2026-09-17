<template>
  <div class="page logs-page">
    <el-form ref="form" class="search" :inline="true" label-position="left" :model="searchFormData">
      <el-form-item label="缓冲垫编号">
        <el-input v-model="searchFormData.qrCode" style="width: 200px;" placeholder="请输入缓冲垫编号" clearable></el-input>
      </el-form-item>
      <el-form-item label="日志类型">
        <el-select v-model="searchFormData.msgType" style="width: 200px;" placeholder="请选择日志类型" clearable>
          <el-option :value="0" label="普通" />
          <el-option :value="1" label="异常 / 提示" />
        </el-select>
      </el-form-item>
      <el-form-item label="日志内容">
        <el-input v-model="searchFormData.msg" style="width: 200px;" placeholder="请输入日志内容" clearable></el-input>
      </el-form-item>
      <el-form-item label="操作结果">
        <el-select v-model="searchFormData.status" clearable placeholder="全部结果" style="width: 160px">
          <el-option v-for="item in resultOptions" :key="item.value" :value="item.value" :label="item.label" />
        </el-select>
      </el-form-item>
      <el-form-item label="扫码器">
        <el-input v-model="searchFormData.scanner" clearable placeholder="名称 / IP / ID / 位置" style="width: 200px" />
      </el-form-item>
      <el-form-item label="PLC">
        <el-input v-model="searchFormData.plc" clearable placeholder="名称 / IP / ID / 位置" style="width: 200px" />
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker
          v-model="searchFormData.timeValue"
          type="datetimerange"
          :picker-options="pickerOptions"
          align="right"
          value-format="yyyy-MM-dd HH:mm:ss"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          clearable
        >
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button class="search_button" type="primary" @click="searchList">查询</el-button>
      </el-form-item>
    </el-form>
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
      :header-cell-style="rowStyle"
    >
      <el-table-column prop="msgType" label="日志类型" width="100">
        <template slot-scope="scope">
          <span :class="logTone(scope.row)">
            <i v-if="logTone(scope.row)" class="el-icon-warning"></i>
            {{ logTypeLabel(scope.row) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="qrCode" width="180" label="缓冲垫编号"></el-table-column>
      <el-table-column label="操作结果" width="110">
        <template slot-scope="scope"><span :class="logTone(scope.row)">{{ resultLabel(scope.row.status) }}</span></template>
      </el-table-column>
      <el-table-column prop="scannerSnapshot" label="扫码器" width="210" show-overflow-tooltip />
      <el-table-column prop="plcSnapshot" label="PLC" width="210" show-overflow-tooltip />
      <el-table-column prop="msg" label="日志内容" min-width="420">
        <template slot-scope="scope">
          <div class="scan-log-message scan-log-preview">{{ scope.row.msg }}</div>
          <el-button v-if="scope.row.msg && scope.row.msg.length > 160" type="text" size="mini" @click="selectedLog = scope.row; detailVisible = true">查看详情</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="createdDate" width="180" label="创建时间" :formatter="formatDate"></el-table-column>
    </el-table>
    <el-pagination
      class="logs-pagination"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="currentPage"
      :page-sizes="pageSizes"
      :page-size="pageSize"
      :disabled="loading"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
    ></el-pagination>
    <el-dialog title="操作日志详情" :visible.sync="detailVisible" width="720px" append-to-body>
      <div class="scan-log-message">{{ selectedLog.msg }}</div>
      <span slot="footer"><el-button @click="detailVisible = false">关闭</el-button></span>
    </el-dialog>
  </div>
</template>
<script>
import { getScanLogs } from '../../modules/log/api';
import dayjs from 'dayjs';
import { isSuccessResponse, responseMessage } from '../../shared/request/request';
import { createPageListMixin } from '../../shared/mixins/page-list';
import { centerCellStyle, tableDateFormatter } from '../../shared/utils/format';
export default {
  name: 'Logs',
  mixins: [createPageListMixin({ pageSize: 20, pageSizes: [20, 50, 100] })],
  data() {
    return {
      resultOptions: [
        { value: 'PROCESSING', label: '处理中' }, { value: 'SUCCESS', label: '成功' },
        { value: 'WARNING', label: '提示' }, { value: 'FAILED', label: '失败' }, { value: 'UNKNOWN', label: '结果未知' }
      ],
      handle: false,
      detailVisible: false,
      selectedLog: {},
      requestId: 0,
      multipleSelection: [],
      searchFormData: {
        qrCode: '',
        timeValue: [dayjs().startOf('day').format('YYYY-MM-DD HH:mm:ss'), dayjs().endOf('day').format('YYYY-MM-DD HH:mm:ss')],
        msgType: null,
        msg: '', status: '', scanner: '', plc: ''
      },
      pickerOptions: {
          shortcuts: [{
            text: '当天',
            onClick(picker) {
              const end = dayjs().endOf('day');
              const start = dayjs().startOf('day');
              picker.$emit('pick', [start.format('YYYY-MM-DD HH:mm:ss'), end.format('YYYY-MM-DD HH:mm:ss')]);
            }
          }, {
            text: '最近三天',
            onClick(picker) {
              const end = dayjs().endOf('day');
              const start = dayjs().subtract(2, 'day').startOf('day');
              picker.$emit('pick', [start.format('YYYY-MM-DD HH:mm:ss'), end.format('YYYY-MM-DD HH:mm:ss')]);
            }
          }, {
            text: '最近七天',
            onClick(picker) {
              const end = dayjs().endOf('day');
              const start = dayjs().subtract(6, 'day').startOf('day');
              picker.$emit('pick', [start.format('YYYY-MM-DD HH:mm:ss'), end.format('YYYY-MM-DD HH:mm:ss')]);
            }
          }]
        }
    };
  },
  methods: {
    resultLabel(status) { const item = this.resultOptions.find(item => item.value === status); return item ? item.label : '历史日志' },
    logTone(row) {
      if (row.status === 'WARNING' || row.status === 'UNKNOWN') return 'scan-log-warning'
      if (row.status === 'FAILED') return 'scan-log-error'
      if (row.status === 'SUCCESS' || row.status === 'PROCESSING') return ''
      return row.msgType === 1 ? 'scan-log-error' : ''
    },
    tableRowClassName({ row }) {
      const tone = this.logTone(row)
      return tone ? `${tone}-row` : ''
    },
    logTypeLabel(row) {
      if (row.status === 'WARNING') return '提示'
      if (row.status === 'UNKNOWN') return '待核实'
      return this.logTone(row) === 'scan-log-error' ? '异常' : '普通'
    },

    formatDate(row, column, cellValue, index) {
      return tableDateFormatter(row, column, cellValue, index);
    },
    refreshLogList() {
      const requestId = ++this.requestId
      this.loading = true
      const params = {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        msg: this.searchFormData.msg,
        status: this.searchFormData.status, scanner: this.searchFormData.scanner, plc: this.searchFormData.plc,
        qrCode: this.searchFormData.qrCode,
        msgType: this.searchFormData.msgType,
        startTime: this.searchFormData.timeValue ? this.searchFormData.timeValue[0] : '',
        endTime: this.searchFormData.timeValue ? this.searchFormData.timeValue[1] : ''
      }
      return getScanLogs(params)
        .then(res => {
          if (requestId !== this.requestId) return
          if (isSuccessResponse(res)) {
            this.setPageResult(res);
          } else {
            throw new Error(responseMessage(res, '日志查询失败'))
          }
        })
        .catch(error => {
          if (requestId !== this.requestId) return
          this.tableData = []
          this.total = 0
          this.handlePageError(error)
        })
        .finally(() => {
          if (requestId === this.requestId) this.loading = false;
        });
    },
    initData() {
      return this.refreshLogList()
    },
    searchList() {
      this.currentPage = 1
      return this.refreshLogList()
    },
    rowStyle() {
      return centerCellStyle();
    }
  },
  mounted() {
    this.refreshLogList()
  },
  beforeDestroy() {
    this.requestId++
  }
};
</script>
<style lang="less">
.scan-log-message { white-space: normal; word-break: break-word; text-align: left; line-height: 1.6; }
.scan-log-preview { display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.logs-pagination {
  margin-top: 16px;
  flex-shrink: 0;
}
.page {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.logs-page .scan-log-warning {
  color: #B7791F;
}
.logs-page .scan-log-error {
  color: #F56C6C;
}
.logs-page .el-table .scan-log-warning-row > td {
  background-color: #FDF6EC;
}
.logs-page .el-table .scan-log-error-row > td {
  background-color: #FEF0F0;
}

.search {
  margin-bottom: 12px;
  // display: flex;
  // justify-content:flex-start;

  .search_button {
    margin-left: 12px;
  }
}

</style>
