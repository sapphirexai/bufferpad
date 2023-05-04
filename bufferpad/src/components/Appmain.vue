<template>
    <div class="main">
      <keep-alive>
      <router-view></router-view>
    </keep-alive></div>
  </template>
  
  <script>
      export default {
          name: "AppMain"
      }
  </script>
  
  <style scoped>
  
  /* 主区域 */
  .main {
    position: absolute;
    top: 50px;
    left: 200px;
    bottom: 0px;
    right: 0px;  /* 距离右边0像素 */
    padding: 10px;
    overflow-y: auto; /* 当内容过多时y轴出现滚动条 */
    /* background-color: red; */
  }
  </style>