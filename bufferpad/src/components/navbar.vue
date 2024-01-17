<template>
  <div class="navbar">
    <el-menu 
      active-text-color="#ffd04b"
      default-active="1"
      background-color="#545c64"
      text-color="#ffffff"
      :router="true"
      :default-active="this.$route.path"
    >
      <el-menu-item v-for="(item, i) in navList" :key="i" :index="item.name">
        <template slot="title">
          <i class="el-icon-s-platform"></i>
          <span> {{ item.navItem }}</span>
        </template>
      </el-menu-item>
    </el-menu>
   
    <!-- 二级菜单 -->
    <!-- <template v-if="!item.leaf">
      <el-submenu :index="index + ''">
        <template slot="title">
          <i :class="item.iconCls"></i>
          <span>{{ item.name }}</span>
        </template>
        <el-menu-item-group>
          <el-menu-item
            :index="child.path"
            :key="index"
            v-for="(child, index) in item.children"
          >
            {{ child.name }}
          </el-menu-item>
        </el-menu-item-group>
      </el-submenu>
    </template> -->
  </div>
</template>

<script>
export default {
  name: "AppNavbar",
  data() {
    return {
      navList: [
        { name: "/index", navItem: "Running" },
        { name: "/summary", navItem: "Summary" }
      ]
    };
  },
  methods: {}
};
</script>

<style scoped>
/* 左侧样式 */
.navbar {
  position: absolute;
  width: 180px;
  top: 50px; /* 距离上面50像素 */
  left: 0px;
  bottom: 0px;
  overflow-y: auto; /* 当内容过多时y轴出现滚动条 */
  background-color: #545c64;
}
</style>
