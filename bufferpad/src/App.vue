<template>
  <div id="app">
    <router-view/>
  </div>
</template>

<script>
export default {
  name: 'App',

mounted() {
    window.addEventListener('offline', () => {
     // 网络由正常常到异常时触发
     sessionStorage.locationUrl = window.location.href;
    //  this.$router.replace('/networkError')
    });
    window.addEventListener('online', () => {
      window.location.href = sessionStorage.locationUrl
    });
  }
}
</script>
