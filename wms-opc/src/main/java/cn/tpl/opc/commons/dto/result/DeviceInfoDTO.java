package cn.tpl.opc.commons.dto.result;

import cn.tpl.opc.commons.constant.Params;
import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/20
 * 设备信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "设备信息实体类")
public class DeviceInfoDTO extends AbsBaseDTO {
    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 设备类型，0: 扫码器；1: PLC
     */
    @Schema(description = "设备类型，0: 扫码器；1: PLC")
    private Integer type;

    /**
     * 设备IP
     */
    @Schema(description = "设备IP")
    private String ip;

    /**
     * 设备端口号
     */
    @Schema(description = "设备端口号")
    private Integer port;

    /**
     * 连接状态
     *
     * @see Params#NETTY_CONNECTION_KEY_STATUS_DISCONNECTED
     * @see Params#NETTY_CONNECTION_KEY_STATUS_ACTIVE
     */
    @Schema(description = "设备状态，0：未连接；1：活跃中")
    private Integer status;

    /**
     * 设备名字
     */
    @Schema(description = "设备名字")
    private String name;

    /**
     * 产线
     */
    @Schema(description = "产线")
    private Integer workLine;

    /**
     * 安装顺序
     */
    @Schema(description = "安装顺序")
    private Integer installSeq;

}
