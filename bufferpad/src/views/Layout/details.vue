<template>
  <div class="page">
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
      <el-table-column prop="qrCode" label="缓冲垫编号"></el-table-column>
      <el-table-column prop="openCount" label="开口数"></el-table-column>
      <el-table-column prop="workLine" label="产线"></el-table-column>
      <el-table-column prop="scannerSeq" label="扫码器安装顺序"></el-table-column>
      <el-table-column prop="createdDate" label="创建时间" :formatter="formatDate"></el-table-column>
    </el-table>
    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page.sync="currentPage"
      :page-sizes="pageSizes"
      :pages-size.sync="pageSize"
      :pager-count="5"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
    >
    </el-pagination>
  </div>
</template>
<script>
import { getDetails } from '../../api';
export default {
  name: 'details',
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
      tableData: [],
      loading: true,
      pageSizes: [8, 15, 20, 100, 10000],
      pageSize: 8,
      currentPage: 1,
      total: 0,
      multipleSelection: [],
      qrCode: ''
    };
  },
  methods: {
    handleSelectionChange(val) {
      this.multipleSelection = val
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

    formatDate(row, column, cellValue, index) {
      var s = new Date(cellValue).toLocaleString();
      return s;
    },
    handleSizeChange(val) {
      this.pageSize = val
      const params = {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        qrCode: this.qrCode
      }
      getDetails(params)
        .then(res => {
          if (res.status === 200) {
            this.tableData = res.data.data.data || [];

            this.total = res.data.data.totalPage || 0
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
    handleCurrentChange(val) {
      this.currentPage = val
      const params = {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        qrCode: this.qrCode
      }
      getDetails(params)
        .then(res => {
          if (res.status === 200) {
            this.tableData = res.data.data.data || [];

            this.total = res.data.data.totalPage || 0
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
    InitpageInfo() {
      const params = {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        qrCode: this.qrCode
      }
      getDetails(params)
      .then(res => {
          console.log('res:', res)
          if (res.status === 200) {
            this.tableData = res.data.data.data || [];

            this.total = res.data.data.totalPage || 0
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
    rowStyle() {
      return 'text-align:center';
    }
  },
  mounted() {
    const href = window.location.href
    let url = href.split('details?')[1]
    const urlParams = new URLSearchParams(url)
    const params = Object.fromEntries(urlParams.entries())
    this.qrCode = params.qrCode
  },

  watch: {
    qrCode: {
      handler(val) {
        if (val) {
          this.InitpageInfo();
        }
      },
      immediate: true
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
  padding: 7.5px 0 !important;
}
</style>
