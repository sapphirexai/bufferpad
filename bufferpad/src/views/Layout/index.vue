<template>
<div>
    <Appheader></Appheader>
    <navbar></navbar>
    <Appmain></Appmain>
  </div>
</template>
<script>
  import Appheader from "@/components/Appheader"
  import navbar from "@/components/navbar"
  import Appmain from "@/components/Appmain"

  // 导入子组件，缩写格式 AppHeader: AppHeader
  export default {
    components: { Appheader, navbar, Appmain }  // 有s
  };

</script>

<style scoped>

  

</style>
