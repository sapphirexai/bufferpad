<template>
  <div>
    <el-table
      :data="tableData"
      style="width: 100%"
      v-loading="loading"
      empty-text="数据等待中"
      element-loading-text="数据玩命加载中"
    >
      <el-table-column prop="id" label="缓冲垫编号" width="180">
      </el-table-column>
      <el-table-column prop="qrCode" label="缓存垫类型" width="180">
      </el-table-column>
      <el-table-column prop="address" label="缓冲垫生成厂家"> </el-table-column>
      <el-table-column prop="createdDate" label="第一次使用时间"> </el-table-column>
      <el-table-column prop="lastScanDate" label="最后一次使用时间">
      </el-table-column>
      <el-table-column prop="address" label="最后一次使用位置">
      </el-table-column>
      <el-table-column prop="usedCount" label="当前使用次数"> </el-table-column>
      <el-table-column prop="maxUseCount" label="剩余次数"> </el-table-column>
    </el-table>
    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="currentPage"
      :page-sizes="pageSizes"
      :pages-size="pageSize"
      :page-size="20"
      :pager-count="7"
      layout="total,sizes,prev, pager, next,jumper"
      :total="total"
    >
    </el-pagination>
  </div>
</template>
<script>
import { getPageInfo } from "../../api";
export default {
  data() {
    return {
      tableData: [],
      loading: true,
      pageSizes: [5, 60, 90, NaN],
      pageSize: 5,
      currentPage: 1,
      total: 0
    };
  },
  methods: {
    getData() {
       getPageInfo(1,10).then(res=>{
        if(res.status==200){
          console.log(res)
          this.tableData = res.data.data.data
          this.total = res.data.data.totalPage
          
          this.loading = false
        }
       
       })
      
     
    },
    handleSizeChange(val) {
      getPageInfo(this.currentPage, val).then(res => {
        if (res.status == 200) {
          console.log(res)
          this.tableData = res.data.data.data
          this.total = res.data.data.totalPage;
          this.loading = false;
        }
      });
    },
    handleCurrentChange(val) {
      getPageInfo(val, this.pageSize).then(res => {
        if (res.status == 200) {
          console.log(res);
          this.tableData = res.data.data.data
          this.total = res.data.data.totalPage;
          this.loading = false;
        }
      });
    }
  },
  mounted() {
    this.getData();
  }
};
</script>
