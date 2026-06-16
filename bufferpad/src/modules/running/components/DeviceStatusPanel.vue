<template>
  <div class="device-status-panel">
    <p class="title">{{ title }}:</p>
    <div class="connection-status">
      <div class="info" v-for="item in devices" :key="item.id">
        <span class="name">{{ item.name }}</span>
        <div
          :class="{ blink: !item.status }"
          :style="{
            width: '14px',
            height: '14px',
            backgroundColor: item.status ? '#67c23a' : 'red',
            borderRadius: '14px'
          }"
        ></div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DeviceStatusPanel',
  props: {
    title: {
      type: String,
      required: true
    },
    devices: {
      type: Array,
      default: () => []
    }
  }
}
</script>

<style scoped lang="less">
.device-status-panel {
  display: flex;
  align-items: center;
}

.title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.connection-status {
  max-height: 120px;
  margin-left: 16px;
  padding: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  .info {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 4px;

    &:last-child {
      margin-bottom: 0;
    }

    .name {
      display: inline-block;
      margin-right: 16px;
    }
  }
}
</style>

