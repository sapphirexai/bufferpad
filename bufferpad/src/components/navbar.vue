<!--
 * @Date         : 2024-04-16 08:55:40
 * @LastEditTime : 2024-04-16 11:12:59
 * @filePath     : no item name
 * @Description  :
 *
 * Copyright (c) 2024 by Jay@lang, All Rights Reserved.
-->
<template>
  <div class="navbar">
    <el-menu
      active-text-color="#ffd04b"
      background-color="#545c64"
      text-color="#ffffff"
      :router="true"
      :default-active='this.$route.path'
    >
      <template v-for="item in navList">
        <el-submenu v-if="item.children" :key="item.name" :index="item.name">
          <template slot="title">
            <i :class="item.icon"></i>
            <span>{{ item.navItem }}</span>
          </template>
          <el-menu-item v-for="child in item.children" :key="child.name" :index="child.name">
            <i :class="child.icon"></i>
            <span>{{ child.navItem }}</span>
          </el-menu-item>
        </el-submenu>
        <el-menu-item v-else :key="item.name" :index="item.name">
        <template slot="title">
          <i :class="item.icon"></i>
          <span> {{ item.navItem }}</span>
        </template>
        </el-menu-item>
      </template>
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
  name: 'AppNavbar',
  computed: {
    navList() {
      const rootRoute = this.$router.options.routes.find(route => route.path === '/')
      const children = rootRoute && rootRoute.children ? rootRoute.children : []
      const groups = {}
      const menus = []

      children
        .filter(route => route.meta && route.meta.menu && (!route.meta.adminOnly || this.$isAdmin))
        .sort((a, b) => (a.meta.order || 0) - (b.meta.order || 0))
        .forEach(route => {
          const meta = route.meta
          if (!meta.parent) {
            menus.push({
              name: route.path,
              navItem: meta.title,
              icon: meta.icon || 'el-icon-menu',
              order: meta.order || 0
            })
            return
          }

          if (!groups[meta.parent]) {
            groups[meta.parent] = {
              name: '/' + meta.parent,
              navItem: meta.parentTitle || meta.parent,
              icon: meta.parentIcon || 'el-icon-setting',
              order: meta.parentOrder || 0,
              children: []
            }
            menus.push(groups[meta.parent])
          }

          groups[meta.parent].children.push({
            name: route.path,
            navItem: meta.title,
            icon: meta.icon || 'el-icon-menu',
            order: meta.order || 0
          })
        })

      menus.forEach(item => {
        if (item.children) item.children.sort((a, b) => (a.order || 0) - (b.order || 0))
      })

      return menus.sort((a, b) => (a.order || 0) - (b.order || 0))
    }
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
