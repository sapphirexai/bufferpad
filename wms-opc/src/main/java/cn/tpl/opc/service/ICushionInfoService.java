package cn.tpl.opc.service;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.CushionDetailDTO;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.request.ModifyCushionInfoScheme;
import cn.tpl.opc.commons.scheme.request.QueryCushionDetailPageScheme;
import cn.tpl.opc.commons.scheme.request.QueryCushionInfoPageScheme;
import cn.tpl.opc.entity.CushionInfoEntity;

import java.util.List;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/17
 * 缓冲垫信息服务接口
 */
public interface ICushionInfoService {

    /**
     * 收到来自扫码器或者人工输入的缓冲垫二维码时调用
     *
     * @param workLine        产线
     * @param scannerHost     扫码器地址
     * @param scannerName     扫码器名字
     * @param scannerPosition 扫码器位置
     * @param scannerSeq      扫码器安装顺序
     * @param qrCode          缓冲垫二维码
     * @return 对应缓冲垫数据
     */
    ResultDTO<CushionInfoDTO> onQrCodeReceived(Integer workLine, String scannerHost, String scannerName, String scannerPosition, Integer scannerSeq, String qrCode);

    /**
     * 分页查询
     *
     * @param scheme 分页查询参数协议
     * @return 分页数据
     */
    PageData<CushionInfoDTO> listByPage(QueryCushionInfoPageScheme scheme);

    /**
     * 新增缓冲垫
     *
     * @param scannerPosition 扫码器位置
     * @param qrCode          缓冲垫二维码
     * @return 添加结果
     */
    boolean add(Integer workLine, String scannerPosition, Integer scannerSeq, String qrCode);

    /**
     * 根据二维码查找缓冲垫
     *
     * @param qrCode 缓冲垫二维码
     * @return 对应缓冲垫信息
     */
    CushionInfoEntity findByQrCode(String qrCode);

    /**
     * 根据二维码模糊查找缓冲垫
     *
     * @param qrCode 缓冲垫二维码
     * @return 对应缓冲垫列表信息
     */
    List<CushionInfoDTO> listByQrCode(String qrCode);


    /**
     * 根据ID列表查找对应缓冲垫明细
     *
     * @param scheme 分页查询参数协议
     * @return 分页数据
     */
    PageData<CushionDetailDTO> listDetailsByPage(QueryCushionDetailPageScheme scheme);

    List<CushionInfoDTO> listByIds(List<Long> ids);

    List<CushionDetailDTO> listDetailsByIds(List<Long> ids);

    /**
     * 修改已使用次数
     *
     * @param cushionInfoEntity 缓冲垫信息
     * @param scannerPosition 扫码器位置
     * @param scannerSeq        扫码器安装顺序
     * @return 修改结果
     */
    boolean modifyUsedCountByQrCode(CushionInfoEntity cushionInfoEntity, String scannerPosition, Integer scannerSeq);

    boolean modifyOpenCountByQrCode(String qrCode, Short openCount);

    boolean modifyMaxUseCountByIds(ModifyCushionInfoScheme scheme);

}
