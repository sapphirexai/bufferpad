/*
 * @Date         : 2024-04-16 08:55:40
 * @LastEditTime : 2024-11-11 11:37:26
 * @filePath     : no item name
 * @Description  : qwe
 *
 * Copyright (c) 2024 by Jay@lang, All Rights Reserved.
 */
import Vue from 'vue'
import Router from 'vue-router'
import Layout from '@/views/layout'
Vue.use(Router)

const router = new Router({
  mode: 'history',
  routes: [
    {
      path: '/',
      name: 'Home',
      component: Layout,
      redirect: '/index',
      children: [
        {
          path: '/summary',
          component: () => import('../views/Layout/summary.vue')
        },
        {
          path: '/index',
          name: 'Running',
          component: () => import('../views/Layout/Running.vue'),
          meta: {
            title: 'running',
            keepAlive: true // 缓存组件
          }
        },
        {
          path: '/record',
          component: () => import('../views/Layout/Record.vue')
        },
        {
          path: '/details',
          component: () => import('../views/Layout/details.vue')
        },
        {
          path: '/logs',
          component: () => import('../views/Layout/logs.vue')
        }
      ]
    },
    {
      path: '/networkError',
      component: require('../components/network_error.vue').default,
      name: 'networkError',
      meta: { title: '网络异常' }
}
  ]
})
// router.beforeEach((to, from, next) => {
//   if (to.meta.keepAlive) { // 判断是否需要缓存组件
//     const cache = sessionStorage.getItem(to.name)
//     if (cache) {
//       to.params = JSON.parse(cache)
//     }
//   }
//   next()
// })

// router.afterEach((to, from) => {
//   if (to.meta.keepAlive) { // 判断是否需要缓存组件
//     sessionStorage.setItem(to.name, JSON.stringify(to.params))
//   }
// })
export default router
