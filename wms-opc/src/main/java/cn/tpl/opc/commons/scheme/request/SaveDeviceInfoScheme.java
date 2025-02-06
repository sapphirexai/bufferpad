package cn.tpl.opc.commons.scheme.request;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.scheme.base.AbsBaseScheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2025/2/6
 * 保存设备信息协议类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "保存设备信息协议类")
public class SaveDeviceInfoScheme extends AbsBaseScheme {
    /**
     * 设备类型，0: 扫码器；1: PLC；2：汇川PLC
     */
    @NotNull(message = Constants.RESULT_MSG_DEVICE_NO_TYPE)
    @Schema(description = "设备类型，0: 扫码器；1: PLC；2：汇川PLC")
    private Integer type;

    /**
     * 设备IP
     */
    @NotBlank(message = Constants.RESULT_MSG_DEVICE_NO_IP)
    @Schema(description = "设备IP")
    private String ip;

    /**
     * 设备端口号
     */
    @NotNull(message = Constants.RESULT_MSG_DEVICE_NO_PORT)
    @Schema(description = "设备端口号")
    private Integer port;

    /**
     * 设备名字
     */
    @Schema(description = "设备名字")
    private String name;

    /**
     * 设备位置
     */
    @Schema(description = "设备位置")
    private String position;

    /**
     * 安装顺序
     */
    @NotNull(message = Constants.RESULT_MSG_DEVICE_NO_INSTALL_SEQ)
    @Schema(description = "安装顺序")
    private Integer installSeq;
}
