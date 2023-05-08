import axios from 'axios'
import router from '../router'
//在开发环境中的测试 development
if (process.env.NODE_ENV == 'development') {
    axios.defaults.baseURL = 'api'
}
//在生产环境中的测试 production
if (process.env.NODE_ENV == 'production') {
    //axios.defaults.baseURL = 'http://192.0.2.1:5000/api'
   
    //重新启动
}
// 还有一种环境 debug
//响应超时的时间
axios.defaults.timeout = 5000
axios.defaults.withCredentials = true;

//接口请求拦截
axios.interceptors.request.use(
    config => {
        config.headers = { DeviceType: 'H5'} //设置响应头部
        return config
    }
)
  
  // 添加响应拦截器
  axios.interceptors.response.use(
    response => {
      // 响应成功处理逻辑
      return response;
    },
    error => {
      // 响应错误处理逻辑
      if (error.response) {
        // 根据状态码判断是否需要跳转到网络错误页面
       
          router.push('/networkError');
      }
      return Promise.reject(error);
    }
  );
export function get(url, params) {
    return new Promise((resolve, reject) => {
        axios.get(url, {
            params: params
        }).then(res => {
            resolve(res)
        }).catch(err => {
            reject(err)
        })
    })
}
export function post(url, params) {
    return new Promise((resolve, reject) => {
        axios.post(url, params)
            .then(res => {
                resolve(res.data)
            })
            .catch(err => {
                reject(err.data)
            })
    })
}   