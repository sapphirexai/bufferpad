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
        <template slot-scope="scope">{{ showCodeName(scope.row.scannerSeq) }}</template>
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
      :pages-size="pageSize"
      :pager-count="5"
      layout="total,sizes,prev, pager, next,jumper"
      :total="total"
    >
    </el-pagination>
  </div>
</template>
<script>
import { getPageInfo } from '../../api';
export default {
  data() {
    return {
      tableData: [],
      loading: true,
      pageSizes: [10, 20, 30],
      pageSize: 10,
      currentPage: 1,
      total: 0
    };
  },
  methods: {
    getData() {
      const params = {
        currentPage: this.currentPage,
        pageSize: this.pageSize
      }
      getPageInfo(params).then(res => {
      // eslint-disable-next-line eqeqeq
        if (res.status == 200) {
          console.log(res)
          this.tableData = res.data.data.data || []
          this.total = res.data.data.totalPage || 0
          this.loading = false
        }
      })
    },
    handleSizeChange(val) {
      getPageInfo(this.currentPage, val).then(res => {
        if (res.status === 200) {
          console.log(res)
          this.tableData = res.data.data.data
          this.total = res.data.data.totalPage;
          this.loading = false;
        }
      });
    },
    handleCurrentChange(val) {
      getPageInfo(val, this.pageSize).then(res => {
        if (res.status === 200) {
          console.log(res);
          this.tableData = res.data.data.data
          this.total = res.data.data.totalPage;
          this.loading = false;
        }
      });
    },
    formatDate(row, column, cellValue, index) {
      // eslint-disable-next-line no-tabs
      var s =	new Date(cellValue).toLocaleString();
      return s;
    },

    rowStyle() {
      return 'text-align:center';
    },
    showCodeName(value) {
      const name = value ? (value === 1 ? '上' : '下') : '-'
      return name
    }
  },
  mounted() {
    this.getData();
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
