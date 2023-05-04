import {get,post} from '../http/index.js'
//封装接口的方法
export let getInfo = ()=>{
    return get('/Production')
}

export let getSummary = ()=>{
    return get('/summary')
}

export let getPLCreadCodeStatus = (id)=>{
     return get('/device/deviceConnections/'+`${id}`
    
     )
}
export let postInfo = (data)=>{
    return post('/addDataById',data)
}
export let getPageInfo = (currentPage,pageSize)=>{
    return get('/cushion/cushionsPage',{
        params:{
            currentPage:currentPage,
            pageSize:pageSize
        }
    })
}