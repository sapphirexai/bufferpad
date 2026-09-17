package cn.tpl.opc.commons.dto.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 设备下拉选项，附带设备类型，供界面展示对应的通信配置提示。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Schema(description = "设备下拉选项")
public class DeviceOptionDTO extends OptionDTO<Long> {
    @Schema(description = "设备类型编码")
    private Integer deviceType;

    public DeviceOptionDTO(Long value, String label, Integer deviceType) {
        super(value, label);
        this.deviceType = deviceType;
    }
}
