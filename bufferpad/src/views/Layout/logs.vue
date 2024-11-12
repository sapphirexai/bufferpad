<template>
  <div class="page">
    <el-form ref="form" class="search" :inline="true" label-position="left" :model="searchFormData">
      <el-form-item label="缓冲垫编号">
        <el-input v-model="searchFormData.qrCode" style="width: 200px;" placeholder="请输入缓冲垫编号" clearable></el-input>
      </el-form-item>
      <el-form-item label="日志类型">
        <el-select v-model="searchFormData.msgType" style="width: 200px;" placeholder="请选择日志类型" clearable>
          <el-option :value="0" label="普通" />
          <el-option :value="1" label="异常" />
        </el-select>
      </el-form-item>
      <el-form-item label="日志内容">
        <el-input v-model="searchFormData.msg" style="width: 200px;" placeholder="请输入日志内容" clearable></el-input>
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
      :cell-class-name="isCheckCell"
      :header-cell-style="rowStyle"
    >
      <el-table-column prop="msgType" label="日志类型" width="180">
        <template slot-scope="scope">
          <i v-if="scope.row.msgType === 1" class="el-icon-warning" style="color: #F56C6C;"></i>
          <span :class="showCodeClass(scope.row.msgType)">{{ showCodeName(scope.row.msgType) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="qrCode" width="280" label="缓冲垫编号"></el-table-column>
      <el-table-column prop="msg" label="日志内容"></el-table-column>
      <el-table-column prop="createdDate" width="280" label="创建时间" :formatter="formatDate"></el-table-column>
    </el-table>
  </div>
</template>
<script>
import { getLogs } from '../../api';
import dayjs from 'dayjs';
export default {
  name: 'Logs',
  data() {
    return {
      handle: false,
      tableData: [],
      loading: true,
      multipleSelection: [],
      searchFormData: {
        qrCode: '',
        timeValue: [dayjs().startOf('day').format('YYYY-MM-DD HH:mm:ss'), dayjs().endOf('day').format('YYYY-MM-DD HH:mm:ss')],
        msgType: null,
        msg: ''
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
    isCheckCell({row, column, rowIndex, columnIndex}) {
      if (row.msgType === 1) {
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

    showCodeName(value) {
      const name = value === 0 ? '普通' : '异常'
      return name
    },
    showCodeClass(value) {
      const className = value === 0 ? '' : 'error'
      return className
    },

    formatDate(row, column, cellValue, index) {
      var s = new Date(cellValue).toLocaleString();
      return s;
    },
    InitpageInfo() {
      const params = {
        msg: this.searchFormData.msg,
        qrCode: this.searchFormData.qrCode,
        msgType: this.searchFormData.msgType,
        startTime: this.searchFormData.timeValue ? this.searchFormData.timeValue[0] : '',
        endTime: this.searchFormData.timeValue ? this.searchFormData.timeValue[1] : ''
      }
      getLogs(params)
      .then(res => {
          console.log('res:', res)
          if (res.status === 200) {
            this.tableData = res.data.data || [];
            this.loading = false;
          }
        })
        .catch(error => {
          if (error.code === 'ECONNABORTED') {
            // 请求超时错误，处理方法
            this.$message.error('请求超时，请稍后再试！');
          } else if (error.message === 'Network Error') {
            // 网络错误，处理方法
            this.$message.error('网络连接异常，请检查您的网络设置！');
          } else {
            // 其他错误，处理方法
            this.$message.error('发生错误：' + error.message);
          }
        });
    },
    searchList() {
      this.InitpageInfo()
    },
    rowStyle() {
      return 'text-align:center';
    }
  },
  mounted() {
    this.InitpageInfo()
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
.error {
  color: #F56C6C;
}

.search {
  margin-bottom: 12px;
  // display: flex;
  // justify-content:flex-start;

  .search_button {
    margin-left: 12px;
  }
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
  background-color: rgba(245, 108, 108, 0.5);
}
</style>
