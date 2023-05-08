<template>
    <div class="network-error">
      <h1>网络错误</h1>
      <p>抱歉，无法连接到服务器，请检查您的网络连接或者后台服务是否启动并重试。</p>
      <button @click="refresh">重试</button>
    </div>
  </template>
  
  <script>
  export default {
    methods: {
      // 点击重试按钮时刷新页面
      refresh() {
        window.location.reload();
      }
    }
  }
  </script>
  
  <style scoped>
  .network-error {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    text-align: center;
  }
  
  .network-error h1 {
    font-size: 3em;
    margin-bottom: 20px;
  }
  
  .network-error button {
    padding: 10px 20px;
    background-color: #00bcd4;
    color: #fff;
    border: none;
    border-radius: 5px;
    cursor: pointer;
    transition: all 0.3s ease;
  }
  
  .network-error button:hover {
    background-color: #008c9e;
  }
  </style>
  