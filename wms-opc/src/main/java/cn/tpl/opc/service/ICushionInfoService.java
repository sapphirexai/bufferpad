package cn.tpl.opc.service;

import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.base.BasePageScheme;
import cn.tpl.opc.entity.CushionInfoEntity;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/17
 * 缓冲垫信息服务接口
 */
public interface ICushionInfoService {

    /**
     * 分页查询
     *
     * @param scheme 分页查询参数协议
     * @return 分页数据
     */
    PageData<CushionInfoDTO> listByPage(BasePageScheme scheme);

    /**
     * 新增缓冲垫
     *
     * @param qrCode 缓冲垫二维码
     * @return 添加结果
     */
    boolean add(String qrCode);

    /**
     * 根据二维码查找缓冲垫
     *
     * @param qrCode 缓冲垫二维码
     * @return 对应缓冲垫信息
     */
    CushionInfoEntity findByQrCode(String qrCode);

    /**
     * 修改已使用次数
     *
     * @param qrCode 缓冲垫二维码
     * @param count  已使用次数
     * @return 修改结果
     */
    boolean modifyUsedCountByQrCode(String qrCode, Integer count);
}
