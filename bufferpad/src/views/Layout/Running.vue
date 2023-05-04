<template>
  <div>
    <el-row
      style="display: flex; justify-content: space-between; align-items: center;"
    >
      <el-col
        :span="5"
        style=" display: flex; align-items: center;justify-content: space-between;"
      >
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
      </el-col>

      <el-col
        :span="3"
        style="display:flex; align-items: center; justify-content: space-between; "
      >
        <p class="title">PLC状态:</p>
        <div
          :style="{
            width: '100px',
            height: '100px',
            backgroundColor:
              devicesMessage && devicesMessage[0].status ? '#67c23a' : 'red',
            borderRadius: '50px'
          }"
        ></div>
      </el-col>
      <el-col
        :span="4"
        style="display:flex; align-items: center; justify-content: space-between; "
      >
        <p class="title">读码器连接状态:</p>
        <div
          :style="{
            width: '100px',
            height: '100px',
            backgroundColor:
              devicesMessage && devicesMessage[0].status ? '#67c23a' : 'red',
            borderRadius: '50px'
          }"
        ></div>
      </el-col>
    </el-row>
    <el-divider></el-divider>
    <el-row
      style="margin:10px 0 10px 0; display: flex; align-items: center; justify-content:space-between;"
      ><el-col :span="5">
        <div class="card" style="flex-direction: column;">
          <p class="title">缓冲垫编号</p>
          <el-input
            v-model="input"
            placeholder="缓冲垫编号"
            :disabled="!handle"
            style="width: 100%; font-size: 20px;padding: 10px;"
          ></el-input>

          <el-button
            type="success"
            style="width: 100%;"
            @click="addItem(handle)"
            >{{ handle ? "确定" : "手动输入" }}</el-button
          >
        </div>
      </el-col>

      <el-col :span="4" style="height: 100%;">
        <div class="card" style="flex-direction:column;">
          <p class="title">使用次数</p>
          <div class="count" style="font-size: 50px;">
            <i class="el-icon-top" style="color: green; font-size:36px ;"></i
            >{{ useCount }}1111
          </div>
        </div>
      </el-col>
      <el-col :span="4" style="height: 100%;">
        <div class="card" style="flex-direction:column;height: 100%;">
          <p class="title">剩余次数</p>
          <div class="count" style="font-size: 50px;">
            <i class="el-icon-bottom" style="color:red;font-size:36px "></i>
            <!-- {{ Count-useCount<=0? '0':Count-useCount}} -->
            2222
          </div>
        </div>
      </el-col>
    </el-row>
    <el-divider></el-divider>
    <el-table
      :data="tableData"
      style="width: 100%"
      :row-class-name="tableRowClassName"
      v-loading="loading"
      empty-text="数据等待中"
      element-loading-text="数据玩命加载中"
    >
      <el-table-column prop="name" label="缓冲垫编号" width="180">
      </el-table-column>
      <el-table-column prop="name" label="缓存垫类型" width="180">
      </el-table-column>
      <el-table-column prop="address" label="第一次使用时间"> </el-table-column>
      <el-table-column prop="address" label="最后一次使用时间">
      </el-table-column>
      <el-table-column prop="address" label="最后一次使用位置">
      </el-table-column>
      <el-table-column prop="address" label="当前使用次数"> </el-table-column>
      <el-table-column prop="address" label="剩余次数"> </el-table-column>
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
      :total="1000"
    >
    </el-pagination>
  </div>
</template>
<style lang="less">
.el-input__inner {
  height: 50px;
  background-color: #fff;
  font-size: 24px;
  font-weight: 800;
}

.el-select-dropdown__item {
  color: #fff;
}

.el-scrollbar,
.el-select-dropdown {
  background-color: transparent !important;
  color: #fff !important;
}

.el-scrollbar__wrap,
.el-select-dropdown__list {
  background-color: #0b1a37;
  color: #fff !important;
}

.el-select-dropdown__item.hover,
.el-select-dropdown__item:hover {
  background-color: rgba(0, 0, 0, 0.3);
  color: #fff;
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
  padding: 20px;
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 5px 5px rgba(0, 0, 0, 0.5);
}

.card .title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}
.title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.card .count {
  font-size: 36px;
  font-weight: bold;
  color: #007bff;
}
/deep/ el-input .el-input__inner {
  background-color: rgba(255, 255, 255, 0.247);
}
</style>
<script>
import {
  getInfo,
  getPLCreadCodeStatus,
  postInfo,
  getPageInfo
} from "../../api";
import EventSourses from "../../api/eventSourse";
export default {
  name: "Running",
  data() {
    return {
      handle: false,
      options: [
        {
          value: "1",
          label: "1"
        },
        {
          value: "2",
          label: "2"
          //   disabled: true
        },
        {
          value: "3",
          label: "3"
        },
        {
          value: "4",
          label: "4"
        },
        {
          value: "5",
          label: "5"
        }
      ],
      ProdLine: "1",
      status: "success",
      tableData: [],
      input: "T202199828232",
      events: null,
      useCount: 111,
      devicesMessage: "",
      loading: true,
      pageSizes: [5, 60, 90, NaN],
      pageSize: 5,
      currentPage: 1,
      total: 0
    };
  },
  methods: {
    tableRowClassName({ row, rowIndex }) {
      if (row.name === "1") {
        return "warning-row";
      }
      return "";
    },
    async getData() {
      let { data: res } = await getInfo();
      this.tableData = res;
    },
    async addItem(flag) {
      if (flag) {
        //调用添加数据到数据库的api
        let res = await postInfo();
        console.log(res);
        this.handle = false;
      } else {
        this.handle = true;
      }
    },
    async subscribeAll() {
      let responses = await Promise.all([getInfo(), getPLCreadCodeStatus()]);

      for (const response of responses) {
        if (response.status == 502) {
          // 状态 502 是连接超时错误，
          // 连接挂起时间过长时可能会发生，
          // 远程服务器或代理会关闭它
          // 让我们重新连接
          await subscribeAll();
        } else if (response.status != 200) {
          // 一个 error —— 让我们显示它
          showMessage(response.statusText);
          // 一秒后重新连接
          await new Promise(resolve => setTimeout(resolve, 1000));
          await subscribeAll();
        } else {
          // 获取并显示消息
          let message = await response.text();
          showMessage(message);
          // 再次调用 subscribe() 以获取下一条消息
          await subscribeAll();
        }
      }
    },
    showMessage(message) {
      console.log(message);
    },
    getPLCreadCodeStatus() {
      getPLCreadCodeStatus(this.ProdLine)
        .then(res => {
          if (res.status === 200) {
            this.devicesMessage = res.data.data;
            console.log(this.devicesMessage);
          }
        })
        .catch(error => {
          if (error.code === "ECONNABORTED") {
            // 请求超时错误，处理方法
            this.$message.error("请求超时，请稍后再试！");
          } else if (error.message === "Network Error") {
            // 网络错误，处理方法
            this.$message.error("网络连接异常，请检查您的网络设置！");
          } else {
            // 其他错误，处理方法
            this.$message.error("发生错误：" + error.message);
          }
        });
    },
    handleSizeChange(val) {
      getPageInfo(val, this.currentPage).then(res => {
        console.log(res);
      });
    },
    handleCurrentChange() {}
  },
  watch: {
    ProdLine: {
      handler(newval, oldval) {
        this.getPLCreadCodeStatus();
      },
      immediate: true
    }
  },
  mounted() {
    this.getPLCreadCodeStatus();
    this.events = new EventSourses(
      "https://gitlab.example.invalid:40570/sse/devicesStatus/" + this.ProdLine,
      function(data) {
        console.log(data);
      }
    );
  },
  beforeDestroy() {
    this.events.close();
  }
};
</script>
