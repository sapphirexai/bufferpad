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
      <el-table-column prop="scannerSeq" label="缓冲垫位置" width="180">
        <template slot-scope="scope">
          <span :class="showCodeClass(scope.row.scannerSeq, scope.row.scannerPosition)">
            {{ showCodeName(scope.row.scannerSeq, scope.row.scannerPosition) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="createdDate" label="扫码时间" :formatter="formatDate"></el-table-column>
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
  </div>
</template>
<script>
import { getCushionDetails } from '../../modules/cushion/api';
import { isSuccessResponse, responseMessage } from '../../shared/request/request';
import { createPageListMixin } from '../../shared/mixins/page-list';
import { centerCellStyle, formatScannerPosition, scannerPositionClass, tableDateFormatter } from '../../shared/utils/format';
export default {
  name: 'details',
  mixins: [createPageListMixin({ pageSize: 8, pageSizes: [8, 15, 20, 100, 10000] })],
  data() {
    return {
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

    showCodeName(value, position) {
      return formatScannerPosition(value, position)
    },
    showCodeClass(value, position) {
      return scannerPositionClass(value, position)
    },

    formatDate(row, column, cellValue, index) {
      return tableDateFormatter(row, column, cellValue, index);
    },
    initData() {
      this.loading = true
      const params = {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        qrCode: this.qrCode
      }
      getCushionDetails(params)
        .then(res => {
          if (isSuccessResponse(res)) {
            this.setPageResult(res)
          } else {
            this.$message.error(responseMessage(res))
          }
        })
        .catch(this.handlePageError)
        .finally(() => {
          this.loading = false
        })
    },
    rowStyle() {
      return centerCellStyle();
    }
  },
  mounted() {
    this.qrCode = this.$route.query.qrCode || ''
  },

  watch: {
    qrCode: {
      handler(val) {
        if (val) {
          this.initData();
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
