const px2rem = require('postcss-px2rem')
const config = require('../config')
module.exports = {
  devServer: {
    proxy: config.dev.proxyTable,
    overlay: {
        warnings: false,
        errors: false
    },

   open:true,
    
    
  },
  css:{
      loaderOptions:{
          sass:{},
          postcss: {                
              plugins: [require('postcss-px2rem')({                    
                  remUnit:192  //设计底稿为1920*1080
              })]
          }
      }    
  }
,
    lintOnSave: false, // eslint-loader 是否在保存的时候检查
  }
// 引入等比适配插件




  
  