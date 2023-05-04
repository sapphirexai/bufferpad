<template>
  <div id="app">
    <router-view/>
  </div>
</template>

<script>
export default {
  name: 'App',
  // created () {
  //   new DevicePixelRatio().init()
  // }
  // mounted(){
  //   //窗口尺寸改变
  //   window.addEventListener("resize", () => this.Refresh());

  // },
  // methods:{
  //   Refresh() {
  //     this.$router.go(0);
  //   },
  // },
  // destroyed() {
  //   // 销毁
  //   window.removeEventListener("resize", this.Refresh());
  // },

}
</script>
