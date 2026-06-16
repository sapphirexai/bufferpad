<template>
  <div class="page">
    <el-table
      :data="tableData"
      style="width: 100%; flex: 1;"
      v-loading="loading"
      empty-text="数据等待中"
      border
      height="800"
      element-loading-text="数据玩命加载中"
      :cell-style="rowStyle"
      :header-cell-style="rowStyle"
    >
      <el-table-column prop="qrCode" label="缓冲垫编号" width="180"></el-table-column>
      <el-table-column prop="scannerSeq" label="缓冲垫位置" width="180">
        <template slot-scope="scope">{{ showCodeName(scope.row.scannerSeq, scope.row.scannerPosition) }}</template>
      </el-table-column>
      <el-table-column prop="openCount" label="开口数" width="180"></el-table-column>
      <el-table-column prop="address" label="缓冲垫生产厂家"></el-table-column>
      <el-table-column prop="createdDate" label="第一次使用时间" :formatter="formatDate"></el-table-column>
      <el-table-column prop="lastScanDate" label="最后一次使用时间" :formatter="formatDate"></el-table-column>
      <el-table-column prop="usedCount" label="当前使用次数"> </el-table-column>
      <el-table-column prop="maxUseCount" label="剩余次数"> </el-table-column>
    </el-table>
    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="currentPage"
      :page-sizes="pageSizes"
      :page-size="pageSize"
      :pager-count="5"
      layout="total,sizes,prev, pager, next,jumper"
      :total="total"
    >
    </el-pagination>
  </div>
</template>
<script>
import { getCushionPage } from '../../modules/cushion/api';
import { isSuccessResponse, responseMessage } from '../../shared/request/request';
import { createPageListMixin } from '../../shared/mixins/page-list';
import { centerCellStyle, formatScannerPosition, tableDateFormatter } from '../../shared/utils/format';
export default {
  mixins: [createPageListMixin({ pageSize: 10, pageSizes: [10, 20, 30] })],
  data() {
    return {};
  },
  methods: {
    initData() {
      this.loading = true
      const params = {
        currentPage: this.currentPage,
        pageSize: this.pageSize
      }
      getCushionPage(params)
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
    formatDate(row, column, cellValue, index) {
      return tableDateFormatter(row, column, cellValue, index);
    },

    rowStyle() {
      return centerCellStyle();
    },
    showCodeName(value, position) {
      return formatScannerPosition(value, position)
    }
  },
  mounted() {
    this.initData();
  }
};
</script>
<style scoped>
.page {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>
