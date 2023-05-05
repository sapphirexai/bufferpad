import {get,post} from '../http/index.js'
//封装接口的方法

export let getPLCreadCodeStatus = (id)=>{
     return get('/device/deviceConnections/'+`${id}`
    
     )
}
export let postInfo = (workline,qrcode)=>{
    return post(`/cushion/manualCushionInfo/${workline}/${qrcode}`)
}
export let getPageInfo = (page,size)=>{
    return get('/cushion/cushionsPage',{
            currentPage:page,
            pageSize:size
    })
}