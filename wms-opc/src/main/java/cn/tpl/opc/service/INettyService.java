package cn.tpl.opc.service;

import cn.tpl.opc.commons.dto.ResultDTO;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/6
 * Netty服务接口
 */
public interface INettyService {
    ResultDTO<Boolean> sendMsg(String ip, Integer port, String msg);

    /**
     * 连接扫码器
     *
     * @return 操作结果
     */
    ResultDTO<Boolean> connectScanner();
}
