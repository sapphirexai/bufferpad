import {get, post} from '../http/index.js'

// 封装接口的方法
export const getPLCreadCodeStatus = (id) => {
    return get('/device/deviceConnections/' + `${id}`)
}

export const qrCodeGetData = (qrCode) => {
    return get('/cushion/cushions/' + `${qrCode}`)
}

export const postInfo = (workline, qrcode) => {
    return post(`/cushion/manualCushionInfo/${workline}/${qrcode}`)
}

export const getPageInfo = (params) => {
    return get('/cushion/cushionsPage', params)
}

export const changeMaxCount = (data) => {
    return post('cushion/cushions', data)
}

export const exportData = (data) => {
    return post('/cushion/cushions/excel', data, 'blob')
}

export const getDetails = (params) => {
    return get('/cushion/detailsPage', params)
}
