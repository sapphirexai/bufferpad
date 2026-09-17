package cn.tpl.opc.commons.dto.result;

import cn.tpl.opc.commons.dto.base.AbsBaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 设备安装位置。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "设备安装位置")
public class DeviceInstallPositionDTO extends AbsBaseDTO {
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "位置名称")
    private String name;

    @Schema(description = "排序号")
    private Integer sortNo;

    @Schema(description = "创建时间")
    private Date createdDate;

    @Schema(description = "更新时间")
    private Date modifiedDate;
}
