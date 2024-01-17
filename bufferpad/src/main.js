/*
 * @Date         : 2023-05-10 09:04:41
 * @LastEditTime : 2024-01-11 17:41:18
 * @FilePath     : /src/main.js
 * @Description  :
 *
 * Copyright (c) 2024 by Jay@lang, All Rights Reserved.
 */
// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import App from './App'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import router from './router'

Vue.use(ElementUI)

Vue.config.productionTip = false

// eslint-disable-next-line no-new
new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
