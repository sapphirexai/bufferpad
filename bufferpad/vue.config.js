const px2rem = require('postcss-px2rem')
const config = require('../config')

module.exports = {
  devServer: {
    proxy: config.dev.proxyTable,
    historyApiFallback: true,
    overlay: {
      warnings: false,
      errors: false
    },
    open: true
  },
  css: {
    loaderOptions: {
      sass: {},
      postcss: {
        plugins: [px2rem({
          remUnit: 192
        })]
      }
    }
  },
  lintOnSave: false
}
