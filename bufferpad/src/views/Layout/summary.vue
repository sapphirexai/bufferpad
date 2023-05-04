<template>
   <div>
      <el-table
    :data="tableData"
    style="width: 100%"
     :row-class-name="tableRowClassName">
    <el-table-column
      prop="name"
      label="缓冲垫编号"
      width="180">
    </el-table-column>
    <el-table-column
      prop="name"
      label="缓存垫类型"
      width="180">
    </el-table-column>
    <el-table-column
      prop="address"
      label="缓冲垫生成厂家">
    </el-table-column>
    <el-table-column
      prop="address"
      label="第一次使用时间">
    </el-table-column>
    <el-table-column
      prop="address"
      label="最后一次使用时间">
    </el-table-column>
    <el-table-column
      prop="address"
      label="最后一次使用位置">
    </el-table-column>
    <el-table-column
      prop="address"
      label="当前使用次数">
    </el-table-column>
    <el-table-column
      prop="address"
      label="剩余次数">
    </el-table-column>
  </el-table>
   </div>
</template>
<script>
      import { getSummary } from '../../api';
      export default {
         data(){
            return{
               tableData:[]
            }
         },
         methods:{
            async getData(){
          let {data:res} = await getSummary()
          this.tableData = res
      }
         },
         mounted(){
            this.getData()
         }
      }
</script>