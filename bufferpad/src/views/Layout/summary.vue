<template>
  <div>
    <el-table
      :data="tableData"
      style="width: 100%"
      v-loading="loading"
      empty-text="数据等待中"
      element-loading-text="数据玩命加载中"
      :cell-style="rowStyle"
      :header-cell-style="rowStyle"
    >
      <el-table-column prop="id" label="缓冲垫编号" width="180">
      </el-table-column>
      <el-table-column prop="qrCode" label="缓存垫类型" width="180">
      </el-table-column>
      <el-table-column prop="address" label="缓冲垫生产厂家"> </el-table-column>
      <el-table-column prop="createdDate" label="第一次使用时间" :formatter="formatDate"> </el-table-column>
      <el-table-column prop="lastScanDate" label="最后一次使用时间" :formatter="formatDate">
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
      :pager-count="5"
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
      pageSizes: [ 10, 20, 30],
      pageSize: 10,
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
    },
    formatDate(row, column, cellValue, index){
		
		//把传过来的日期进行回炉重造一下，又创建了一个js的 Date对象，进行重新构造，转为String字符串
		//最终返回 s 就可以了
		var s =	new Date(cellValue).toLocaleString();
	    return s;
		  // 测试参数含义：不知道参数是什么含义的就打印出来
		  // console.log(row)     	//拿到一行的所有信息，要拿到具体信息,只需要row.XXX 就可以了
		  // console.log(column)  	//拿到列的信息
		  // console.log(cellValue) //拿到单元格数据，拿到时间 2022-03-18T01:46:08.000+00:00
		  // console.log(index)     //拿到索引
	    },

      rowStyle() {
      return "text-align:center";
    },
  },
  mounted() {
    this.getData();
  }
};
</script>
