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
          component: () => import('../views/Layout/summary.vue'),
          meta: {
            title: '缓冲垫汇总',
            icon: 'el-icon-s-data',
            menu: true,
            order: 2
          }
        },
        {
          path: '/index',
          name: 'Running',
          component: () => import('../views/Layout/Running.vue'),
          meta: {
            title: '运行监控',
            icon: 'el-icon-s-platform',
            menu: true,
            order: 1,
            keepAlive: true
          }
        },
        {
          path: '/record',
          component: () => import('../views/Layout/Record.vue'),
          meta: {
            title: '使用记录',
            icon: 'el-icon-document',
            menu: true,
            order: 3
          }
        },
        {
          path: '/details',
          component: () => import('../views/Layout/details.vue'),
          meta: {
            title: '缓冲垫明细',
            menu: false
          }
        },
        {
          path: '/logs',
          component: () => import('../views/Layout/logs.vue'),
          meta: {
            title: '日志查询',
            menu: false
          }
        },
        {
          path: '/settings/lifespan',
          component: () => import('../views/Layout/settings/Lifespan.vue'),
          meta: {
            title: '缓冲垫寿命',
            icon: 'el-icon-timer',
            menu: true,
            parent: 'settings',
            parentTitle: '设置',
            parentIcon: 'el-icon-setting',
            parentOrder: 9,
            order: 1
          }
        },
        {
          path: '/settings/devices',
          component: () => import('../views/Layout/settings/Devices.vue'),
          meta: {
            title: '设备信息',
            icon: 'el-icon-monitor',
            menu: true,
            parent: 'settings',
            parentTitle: '设置',
            parentIcon: 'el-icon-setting',
            parentOrder: 9,
            order: 2
          }
        },
        {
          path: '/settings/plc-addresses',
          component: () => import('../views/Layout/settings/PLCAddresses.vue'),
          meta: {
            title: 'PLC地址',
            icon: 'el-icon-connection',
            menu: true,
            parent: 'settings',
            parentTitle: '设置',
            parentIcon: 'el-icon-setting',
            parentOrder: 9,
            order: 3
          }
        },
        {
          path: '/settings/install-positions',
          component: () => import('../views/Layout/settings/InstallPositions.vue'),
          meta: {
            title: '设备安装位置',
            icon: 'el-icon-location-outline',
            menu: true,
            parent: 'settings',
            parentTitle: '设置',
            parentIcon: 'el-icon-setting',
            parentOrder: 9,
            order: 4
          }
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
