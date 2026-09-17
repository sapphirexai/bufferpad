package cn.tpl.opc.commons.scheme.request;

import cn.tpl.opc.commons.scheme.base.AbsBaseScheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * 保存设备安装位置。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "保存设备安装位置")
public class SaveDeviceInstallPositionScheme extends AbsBaseScheme {
    @Schema(description = "主键ID，编辑时传入")
    private Long id;

    @NotBlank(message = "安装位置名称不能为空！")
    @Schema(description = "位置名称")
    private String name;

    @Schema(description = "排序号")
    private Integer sortNo;
}
