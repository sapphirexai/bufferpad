import Vue from 'vue'
import Router from 'vue-router'
import Layout from '@/views/layout'
Vue.use(Router)

const router =  new Router({
  mode: 'history', 
  routes: [
    {
      path: '/',
      name: 'Home',
      component: Layout,
      redirect:'/index',
      children:[
        {path: '/summary',component: () => import('../views/Layout/summary.vue')},
        {path:'/index',
        name:'Running',
        component:()=>import('../views/Layout/Running.vue'),
        meta: {
          title: 'running',
          keepAlive: true // 缓存组件
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